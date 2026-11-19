import 'package:flutter/foundation.dart';

/// Pestaña activa del inicio (0 catálogo, 1 historial, 2 cuenta).
class NavController extends ChangeNotifier {
  int index = 0;

  void ir(int i) {
    if (i == index) return;
    index = i;
    notifyListeners();
  }
}
