import 'package:flutter/material.dart';

import '../../core/utils/formatters.dart';
import '../../core/utils/logos.dart';

/// Logotipo del proveedor, igual que en la web: primero el logo guardado en la app (assets/logos), luego la URL
/// que mande Gestopago y, si ninguno carga, las iniciales.
class ProveedorLogo extends StatelessWidget {
  const ProveedorLogo({super.key, required this.nombre, this.url, this.size = 48, this.otrosNombres = const []});

  final String nombre;
  final String? url;
  final double size;

  /// Otros textos donde buscar la marca (p. ej. el nombre del producto).
  final List<String?> otrosNombres;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final fallback = Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: scheme.primaryContainer,
        borderRadius: BorderRadius.circular(size * 0.25),
      ),
      child: Text(
        initials(nombre),
        style: TextStyle(
          color: scheme.onPrimaryContainer,
          fontWeight: FontWeight.w700,
          fontSize: size * 0.34,
        ),
      ),
    );

    final local = Logos.asset([nombre, ...otrosNombres]);
    if (local != null) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(size * 0.25),
        child: Image.asset(
          local,
          width: size,
          height: size,
          fit: BoxFit.cover,
          semanticLabel: 'Logotipo de $nombre',
          errorBuilder: (_, __, ___) => fallback,
        ),
      );
    }

    final u = url;
    if (u == null || u.isEmpty) return fallback;

    return ClipRRect(
      borderRadius: BorderRadius.circular(size * 0.25),
      child: Container(
        width: size,
        height: size,
        color: Colors.white,
        padding: EdgeInsets.all(size * 0.08),
        child: Image.network(
          u,
          fit: BoxFit.contain,
          semanticLabel: 'Logotipo de $nombre',
          errorBuilder: (_, __, ___) => fallback,
          loadingBuilder: (context, child, progress) => progress == null
              ? child
              : const Center(child: SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))),
        ),
      ),
    );
  }
}
