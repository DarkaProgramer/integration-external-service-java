import 'package:flutter/material.dart';

import 'app_text_field.dart';

/// Lista para elegir un valor con la opción "Otro (escribir)…" (igual que la web). Si el valor no está en la
/// lista, o no hay lista, muestra un campo de texto normal para escribirlo a mano.
class SelectorConOtro extends StatefulWidget {
  const SelectorConOtro({
    super.key,
    required this.controller,
    required this.label,
    required this.opciones,
    this.validator,
    this.onChanged,
    this.enabled = true,
    this.hint,
    this.cargando = false,
  });

  final TextEditingController controller;
  final String label;
  final List<String> opciones;
  final FormFieldValidator<String>? validator;
  final ValueChanged<String>? onChanged;
  final bool enabled;
  final String? hint;
  final bool cargando;

  @override
  State<SelectorConOtro> createState() => _SelectorConOtroState();
}

class _SelectorConOtroState extends State<SelectorConOtro> {
  late bool _manual = widget.controller.text.isNotEmpty && !widget.opciones.contains(widget.controller.text);

  @override
  void didUpdateWidget(SelectorConOtro old) {
    super.didUpdateWidget(old);
    // Cambió la lista (otro país u otro estado): se vuelve a ofrecer la lista
    if (old.opciones != widget.opciones) {
      _manual = widget.controller.text.isNotEmpty && !widget.opciones.contains(widget.controller.text);
    }
  }

  void _poner(String v) {
    widget.controller.text = v;
    widget.onChanged?.call(v);
  }

  Future<void> _abrirLista() async {
    final elegido = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => _ListaBuscable(titulo: widget.label, opciones: widget.opciones),
    );
    if (elegido == null) return;
    if (elegido == _ListaBuscable.otro) {
      setState(() => _manual = true);
      _poner('');
    } else {
      _poner(elegido);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.opciones.isEmpty || _manual) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AppTextField(
            controller: widget.controller,
            label: widget.label,
            hint: widget.cargando ? 'Cargando lista…' : widget.hint ?? 'Escríbelo',
            enabled: widget.enabled,
            validator: widget.validator,
            onChanged: widget.onChanged,
            textCapitalization: TextCapitalization.words,
          ),
          if (widget.opciones.isNotEmpty)
            Transform.translate(
              offset: const Offset(0, -10),
              child: TextButton(
                onPressed: () {
                  setState(() => _manual = false);
                  _poner('');
                },
                child: const Text('¿Está en la lista? Elegir de la lista'),
              ),
            ),
        ],
      );
    }
    return AppTextField(
      controller: widget.controller,
      label: widget.label,
      hint: widget.hint ?? 'Selecciona…',
      helper: '¿No aparece? Elige "Otro (escribir)" al final.',
      enabled: widget.enabled,
      readOnly: true,
      onTap: widget.enabled ? _abrirLista : null,
      suffix: const Icon(Icons.arrow_drop_down),
      validator: widget.validator,
    );
  }
}

class _ListaBuscable extends StatefulWidget {
  const _ListaBuscable({required this.titulo, required this.opciones});

  static const otro = '\u0000otro';
  final String titulo;
  final List<String> opciones;

  @override
  State<_ListaBuscable> createState() => _ListaBuscableState();
}

class _ListaBuscableState extends State<_ListaBuscable> {
  String _filtro = '';

  static String _normal(String s) => s
      .toLowerCase()
      .replaceAll(RegExp('[áä]'), 'a')
      .replaceAll(RegExp('[éë]'), 'e')
      .replaceAll(RegExp('[íï]'), 'i')
      .replaceAll(RegExp('[óö]'), 'o')
      .replaceAll(RegExp('[úü]'), 'u');

  @override
  Widget build(BuildContext context) {
    final f = _normal(_filtro);
    final visibles = widget.opciones.where((o) => _normal(o).contains(f)).toList();
    return SizedBox(
      height: MediaQuery.of(context).size.height * 0.75,
      child: Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: TextField(
                autofocus: widget.opciones.length > 12,
                decoration: InputDecoration(
                  labelText: 'Buscar ${widget.titulo.replaceAll('*', '').trim().toLowerCase()}',
                  prefixIcon: const Icon(Icons.search),
                ),
                onChanged: (v) => setState(() => _filtro = v),
              ),
            ),
            Expanded(
              child: ListView.builder(
                itemCount: visibles.length + 1,
                itemBuilder: (_, i) => i == visibles.length
                    ? ListTile(
                        leading: const Icon(Icons.edit_outlined),
                        title: const Text('Otro (escribir)…'),
                        onTap: () => Navigator.pop(context, _ListaBuscable.otro),
                      )
                    : ListTile(title: Text(visibles[i]), onTap: () => Navigator.pop(context, visibles[i])),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
