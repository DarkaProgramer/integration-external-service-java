import { useCallback, useEffect, useRef, useState } from 'react';
import { authApi } from '../api/authApi';
import { config } from '../config/env';

/**
 * Consulta GET /api/auth/alertas cada ~20 s mientras la pestaña está visible.
 * Devuelve las alertas pendientes y acciones para descartarlas o cerrar la otra sesión.
 */
export function useAlertasSesion(activo) {
  const [alertas, setAlertas] = useState([]);
  const timer = useRef(null);

  const consultar = useCallback(async () => {
    if (document.hidden) return;
    try {
      const nuevas = await authApi.alertas();
      setAlertas(nuevas);
    } catch {
      /* un fallo de polling no interrumpe al usuario; se reintenta en el siguiente ciclo */
    }
  }, []);

  useEffect(() => {
    if (!activo) {
      setAlertas([]);
      return undefined;
    }
    consultar();
    timer.current = setInterval(consultar, config.alertasIntervaloMs);
    const alVolver = () => !document.hidden && consultar();
    document.addEventListener('visibilitychange', alVolver);
    return () => {
      clearInterval(timer.current);
      document.removeEventListener('visibilitychange', alVolver);
    };
  }, [activo, consultar]);

  const descartar = useCallback(async () => {
    await authApi.marcarAlertasLeidas();
    setAlertas([]);
  }, []);

  const cerrarSesionRemota = useCallback(
    async (sesionId) => {
      await authApi.cerrarSesion(sesionId);
      const restantes = alertas.filter((a) => a.sesionId !== sesionId);
      if (restantes.length === 0) await descartar();
      else setAlertas(restantes);
    },
    [alertas, descartar],
  );

  return { alertas, descartar, cerrarSesionRemota };
}
