import 'package:flutter/material.dart';

import '../../core/utils/formatters.dart';
import '../../data/models/producto.dart';
import 'proveedor_logo.dart';

/// Tarjeta de un producto del catálogo: logo, nombre, proveedor, categoría, descripción y precio.
class ProductoCard extends StatelessWidget {
  const ProductoCard({super.key, required this.producto, required this.onTap});

  final Producto producto;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final p = producto;
    final precio = p.montoVariable
        ? (p.montoMinimo != null && p.montoMaximo != null
            ? '${formatMoney(p.montoMinimo)} – ${formatMoney(p.montoMaximo)}'
            : 'Monto variable')
        : formatMoney(p.precio);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  ProveedorLogo(nombre: p.servicio.isEmpty ? p.nombre : p.servicio, url: p.logoUrl, size: 48, otrosNombres: [p.nombre]),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(p.nombre, maxLines: 2, overflow: TextOverflow.ellipsis, style: theme.textTheme.titleSmall),
                        if (p.servicio.isNotEmpty)
                          Text(p.servicio,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              if ((p.descripcion ?? '').isNotEmpty)
                Text(p.descripcion!, maxLines: 2, overflow: TextOverflow.ellipsis, style: theme.textTheme.bodySmall),
              const Spacer(),
              Row(
                children: [
                  Flexible(
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.secondaryContainer,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(p.categoria,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.labelSmall?.copyWith(color: theme.colorScheme.onSecondaryContainer)),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(precio, style: theme.textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w700)),
                ],
              ),
              const SizedBox(height: 4),
              Text('ID ${p.identificador}', style: theme.textTheme.labelSmall?.copyWith(color: theme.colorScheme.outline)),
            ],
          ),
        ),
      ),
    );
  }
}
