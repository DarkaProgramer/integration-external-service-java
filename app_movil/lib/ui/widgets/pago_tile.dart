import 'package:flutter/material.dart';

import '../../core/utils/formatters.dart';
import '../../data/models/pago.dart';
import 'common.dart';
import 'proveedor_logo.dart';

/// Renglón del historial: logo, servicio, fecha, referencia, importe, folio y estado.
class PagoTile extends StatelessWidget {
  const PagoTile({super.key, required this.pago, this.logoUrl, required this.onTap});

  final Pago pago;
  final String? logoUrl;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final nombre = pago.servicio ?? pago.producto ?? 'Servicio';
    final muted = theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ProveedorLogo(nombre: nombre, url: logoUrl, size: 44, otrosNombres: [pago.producto]),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(nombre,
                              maxLines: 1, overflow: TextOverflow.ellipsis, style: theme.textTheme.titleSmall),
                        ),
                        Text(formatMoney(pago.monto),
                            style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700)),
                      ],
                    ),
                    if (pago.producto != null && pago.producto != nombre)
                      Text(pago.producto!, maxLines: 1, overflow: TextOverflow.ellipsis, style: muted),
                    const SizedBox(height: 4),
                    Text('${formatDateTime(pago.fechaOperacion)} · Ref. ${pago.referencia ?? '—'}', style: muted),
                    Text('Folio ${pago.folio}', maxLines: 1, overflow: TextOverflow.ellipsis, style: muted),
                    const SizedBox(height: 6),
                    ResultadoChip(resultado: pago.resultado),
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
