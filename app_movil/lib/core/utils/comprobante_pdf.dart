import 'dart:typed_data';

import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;

import '../../data/models/pago.dart';
import 'formatters.dart';

/// Comprobante de un pago exitoso en PDF (equivalente a "Imprimir o guardar comprobante" de la web).
class ComprobantePdf {
  ComprobantePdf._();

  static Future<Uint8List> generar(Pago p) async {
    final doc = pw.Document(title: 'Comprobante ${p.folio}', author: 'Hasani Pagos');
    final filas = <(String, String?)>[
      ('Estado', p.resultado.label),
      ('Folio', p.folio),
      ('Folio del proveedor', p.folioProveedor),
      ('Servicio', p.servicio),
      ('Producto', p.producto),
      ('Referencia', p.referencia),
      ('Forma de pago', p.tarjeta == null ? null : 'Tarjeta de débito ${p.tarjeta} · contado'),
      ('Autorización bancaria', p.autorizacion),
      ('Fecha', formatDateTime(p.fechaOperacion)),
    ].where((f) => (f.$2 ?? '').isNotEmpty).toList();

    doc.addPage(
      pw.Page(
        pageFormat: PdfPageFormat.letter,
        margin: const pw.EdgeInsets.all(48),
        build: (_) => pw.Column(
          crossAxisAlignment: pw.CrossAxisAlignment.start,
          children: [
            pw.Text('Hasani Pagos', style: const pw.TextStyle(fontSize: 22, fontWeight: pw.FontWeight.bold)),
            pw.Text('Comprobante de pago de servicio'),
            pw.SizedBox(height: 24),
            pw.Text(formatMoney(p.monto), style: const pw.TextStyle(fontSize: 32, fontWeight: pw.FontWeight.bold)),
            pw.SizedBox(height: 16),
            pw.TableHelper.fromTextArray(
              data: [for (final f in filas) [f.$1, f.$2!]],
              border: null,
              cellStyle: const pw.TextStyle(fontSize: 12),
              cellAlignments: {0: pw.Alignment.centerLeft, 1: pw.Alignment.centerRight},
              rowDecoration: const pw.BoxDecoration(
                border: pw.Border(bottom: pw.BorderSide(color: PdfColors.grey300, width: 0.5)),
              ),
            ),
          ],
        ),
      ),
    );
    return doc.save();
  }
}
