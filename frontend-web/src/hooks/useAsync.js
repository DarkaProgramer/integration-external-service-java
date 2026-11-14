import { useCallback, useEffect, useRef, useState } from 'react';

/** Carga datos con estados de carga/error y una función para recargar. */
export function useAsync(fn, deps = []) {
  const [estado, setEstado] = useState({ cargando: true, error: null, datos: null });
  const montado = useRef(true);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const ejecutar = useCallback(fn, deps);

  const cargar = useCallback(async () => {
    setEstado((e) => ({ ...e, cargando: true, error: null }));
    try {
      const datos = await ejecutar();
      if (montado.current) setEstado({ cargando: false, error: null, datos });
    } catch (error) {
      if (montado.current) setEstado((e) => ({ ...e, cargando: false, error }));
    }
  }, [ejecutar]);

  useEffect(() => {
    montado.current = true;
    cargar();
    return () => {
      montado.current = false;
    };
  }, [cargar]);

  return { ...estado, recargar: cargar };
}
