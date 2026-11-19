import 'package:flutter/material.dart';

import '../../data/models/pago.dart';

/// Limita el ancho del contenido en tabletas y pantallas grandes.
class ResponsiveBody extends StatelessWidget {
  const ResponsiveBody({super.key, required this.child, this.maxWidth = 640, this.padding = const EdgeInsets.all(16)});

  final Widget child;
  final double maxWidth;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: Padding(padding: padding, child: child),
      ),
    );
  }
}

/// Botón principal que muestra un indicador mientras se procesa.
class LoadingButton extends StatelessWidget {
  const LoadingButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.loading = false,
    this.icon,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool loading;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final child = loading
        ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5))
        : Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (icon != null) ...[Icon(icon), const SizedBox(width: 8)],
              Flexible(child: Text(label, overflow: TextOverflow.ellipsis)),
            ],
          );
    return SizedBox(
      width: double.infinity,
      height: 52,
      child: FilledButton(onPressed: loading ? null : onPressed, child: child),
    );
  }
}

/// Fila etiqueta / valor para resúmenes y comprobantes.
class InfoRow extends StatelessWidget {
  const InfoRow({super.key, required this.label, required this.value, this.emphasize = false, this.selectable = false});

  final String label;
  final String value;
  final bool emphasize;
  final bool selectable;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final style = emphasize
        ? theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)
        : theme.textTheme.bodyLarge;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(label, style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 3,
            child: selectable
                ? SelectableText(value, textAlign: TextAlign.end, style: style)
                : Text(value, textAlign: TextAlign.end, style: style),
          ),
        ],
      ),
    );
  }
}

/// Tarjeta con título para agrupar información.
class SectionCard extends StatelessWidget {
  const SectionCard({super.key, this.title, required this.children});

  final String? title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (title != null) ...[
              Text(title!, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 4),
            ],
            ...children,
          ],
        ),
      ),
    );
  }
}

/// Colores e íconos de cada resultado de pago (los mismos en toda la app).
extension ResultadoPagoUi on ResultadoPago {
  Color color(BuildContext context) => switch (this) {
        ResultadoPago.exitoso => const Color(0xFF1B873F),
        ResultadoPago.rechazado => Theme.of(context).colorScheme.error,
        ResultadoPago.pendiente => const Color(0xFFB26A00),
      };

  IconData get icon => switch (this) {
        ResultadoPago.exitoso => Icons.check_circle,
        ResultadoPago.rechazado => Icons.cancel,
        ResultadoPago.pendiente => Icons.schedule,
      };
}

class ResultadoChip extends StatelessWidget {
  const ResultadoChip({super.key, required this.resultado});

  final ResultadoPago resultado;

  @override
  Widget build(BuildContext context) {
    final c = resultado.color(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: c.withOpacity(0.12),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(resultado.icon, size: 16, color: c),
          const SizedBox(width: 4),
          Text(resultado.label, style: TextStyle(color: c, fontWeight: FontWeight.w600, fontSize: 12)),
        ],
      ),
    );
  }
}
