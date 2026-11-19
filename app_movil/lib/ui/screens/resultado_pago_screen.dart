import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:printing/printing.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/comprobante_pdf.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/pago.dart';
import '../../data/repositories/pagos_repository.dart';
import '../../state/historial_controller.dart';
import '../../state/nav_controller.dart';
import '../widgets/common.dart';
import '../widgets/proveedor_logo.dart';
import '../widgets/state_views.dart';

/// Comprobante de un pago. Se usa al terminar de pagar y como detalle desde el historial.
class ResultadoPagoScreen extends StatefulWidget {
  const ResultadoPagoScreen({super.key, required this.pago, this.logoUrl, this.desdeHistorial = false});

  final Pago pago;
  final String? logoUrl;
  final bool desdeHistorial;

  @override
  State<ResultadoPagoScreen> createState() => _ResultadoPagoScreenState();
}

class _ResultadoPagoScreenState extends State<ResultadoPagoScreen> {
  late Pago _pago = widget.pago;
  bool _consultando = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    // En el detalle se consulta el folio para mostrar el estado más reciente.
    if (widget.desdeHistorial) WidgetsBinding.instance.addPostFrameCallback((_) => _consultar());
  }

  Future<void> _consultar() async {
    setState(() {
      _consultando = true;
      _error = null;
    });
    try {
      final p = await context.read<PagosRepository>().consultar(_pago.folio);
      if (!mounted) return;
      setState(() => _pago = p);
      context.read<HistorialController>().actualizar(p);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _consultando = false);
    }
  }

  void _irA(int tab) {
    context.read<NavController>().ir(tab);
    Navigator.of(context).popUntil((r) => r.isFirst);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final p = _pago;
    final color = p.resultado.color(context);
    final titulo = switch (p.resultado) {
      ResultadoPago.exitoso => 'Pago exitoso',
      ResultadoPago.rechazado => 'Pago rechazado',
      ResultadoPago.pendiente => 'Pago pendiente de confirmación',
    };
    final nombre = p.servicio ?? p.producto ?? 'Servicio';

    // Después de pagar, "atrás" regresa al inicio (no a la captura de la referencia ya usada).
    return PopScope(
      canPop: widget.desdeHistorial,
      onPopInvoked: (didPop) {
        if (!didPop) Navigator.of(context).popUntil((r) => r.isFirst);
      },
      child: Scaffold(
      appBar: AppBar(
        title: Text(widget.desdeHistorial ? 'Detalle del pago' : 'Resultado'),
        automaticallyImplyLeading: widget.desdeHistorial,
        actions: [
          IconButton(
            tooltip: 'Actualizar estado',
            onPressed: _consultando ? null : _consultar,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          children: [
            ResponsiveBody(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_consultando) const LinearProgressIndicator(),
                  const SizedBox(height: 8),
                  Icon(p.resultado.icon, size: 80, color: color),
                  const SizedBox(height: 8),
                  Text(titulo,
                      textAlign: TextAlign.center,
                      style: theme.textTheme.headlineSmall?.copyWith(color: color, fontWeight: FontWeight.w700)),
                  if ((p.mensaje ?? '').isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Text(p.mensaje!, textAlign: TextAlign.center, style: theme.textTheme.bodyMedium),
                  ],
                  const SizedBox(height: 16),
                  if (_error != null) ...[
                    MessageBanner(message: _error!),
                    const SizedBox(height: 12),
                  ],
                  SectionCard(
                    children: [
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: ProveedorLogo(nombre: nombre, url: widget.logoUrl, otrosNombres: [p.producto]),
                        title: Text(nombre),
                        subtitle: p.producto != null && p.producto != nombre ? Text(p.producto!) : null,
                        trailing: ResultadoChip(resultado: p.resultado),
                      ),
                      const Divider(),
                      InfoRow(label: 'Importe', value: formatMoney(p.monto), emphasize: true),
                      InfoRow(label: 'Referencia', value: p.referencia ?? '—'),
                      InfoRow(label: 'Fecha', value: formatDateTime(p.fechaOperacion)),
                      InfoRow(label: 'Folio', value: p.folio, selectable: true),
                      if ((p.folioProveedor ?? '').isNotEmpty)
                        InfoRow(label: 'Folio del proveedor', value: p.folioProveedor!, selectable: true),
                      if ((p.tarjeta ?? '').isNotEmpty)
                        InfoRow(label: 'Forma de pago', value: 'Tarjeta de débito ${p.tarjeta} · contado'),
                      if ((p.autorizacion ?? '').isNotEmpty)
                        InfoRow(label: 'Autorización bancaria', value: p.autorizacion!, selectable: true),
                      if (p.estatus != null) InfoRow(label: 'Estatus', value: p.estatus!),
                    ],
                  ),
                  const SizedBox(height: 8),
                  TextButton.icon(
                    onPressed: () {
                      Clipboard.setData(ClipboardData(text: p.folio));
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Folio copiado')));
                    },
                    icon: const Icon(Icons.copy),
                    label: const Text('Copiar folio'),
                  ),
                  if (p.resultado == ResultadoPago.exitoso)
                    TextButton.icon(
                      // Abre el diálogo de impresión del sistema: imprimir o guardar como PDF (igual que la web)
                      onPressed: () => Printing.layoutPdf(
                        name: 'comprobante-${p.folio}.pdf',
                        onLayout: (_) => ComprobantePdf.generar(p),
                      ),
                      icon: const Icon(Icons.picture_as_pdf_outlined),
                      label: const Text('Imprimir o guardar comprobante (PDF)'),
                    ),
                  if (p.resultado == ResultadoPago.pendiente) ...[
                    const MessageBanner(
                      kind: BannerKind.warning,
                      message: 'El proveedor aún no confirma este pago. El monto queda retenido; '
                          'consulta el estado más tarde con el botón de actualizar.',
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      onPressed: _consultando ? null : _consultar,
                      icon: const Icon(Icons.sync),
                      label: const Text('Consultar estado'),
                    ),
                  ],
                  const SizedBox(height: 16),
                  if (!widget.desdeHistorial) ...[
                    LoadingButton(label: 'Ver historial', icon: Icons.receipt_long, onPressed: () => _irA(1)),
                    const SizedBox(height: 8),
                    TextButton(onPressed: () => _irA(0), child: const Text('Volver al catálogo')),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
      ),
    );
  }
}
