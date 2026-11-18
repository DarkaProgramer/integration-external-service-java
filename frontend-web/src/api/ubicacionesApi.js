import { PAISES, MEXICO } from '../data/paises';

// Catálogo de ubicaciones para el registro.
// - México: 32 entidades y sus municipios/alcaldías (claves y nombres de INEGI tomados del catálogo de
//   códigos postales de SEPOMEX). Va incluido en el frontend y se descarga solo al abrir el paso del
//   domicilio, así no depende de una API externa que pueda caerse, cambiar o pedir llave.
// - Otros países: lista ISO 3166 con nombres en español. Para ellos, estado y municipio se escriben a mano.
let mexico = null;

async function cargarMexico() {
  if (!mexico) {
    mexico = import('../data/mexico.json').then((m) => m.default ?? m).catch((e) => {
      mexico = null; // permite reintentar si falló la descarga del chunk
      throw e;
    });
  }
  return mexico;
}

export const ubicacionesApi = {
  paises: () => PAISES,
  esMexico: (pais) => (pais ?? '').trim().toLowerCase() === MEXICO.toLowerCase(),
  /** [{ clave, nombre, municipios: [nombre] }] */
  estadosDeMexico: cargarMexico,
};
