// Logos de las compañías, guardados en /public/logos (no se cargan de sitios externos).
// Para cambiar uno, reemplaza el archivo con el mismo nombre (SVG o cambia la extensión aquí).
const MARCAS = [
  [/telcel/i, 'telcel', '#0033A1'],
  [/at&t|at ?& ?t|\batt\b/i, 'att', '#009FDB'],
  [/movistar/i, 'movistar', '#019DF4'],
  [/\bbait\b/i, 'bait', '#FFD100', '#0b2a6b'],
  [/unefon/i, 'unefon', '#FFC20E', '#1f1f1f'],
  [/virgin/i, 'virgin', '#E10A0A'],
  [/\bcfe\b|comisi[oó]n federal/i, 'cfe', '#00843D'],
  [/agua/i, 'agua', '#0EA5E9'],
  [/telmex/i, 'telmex', '#0033A0'],
  [/izzi/i, 'izzi', '#E5007E'],
  [/totalplay/i, 'totalplay', '#00A859'],
  [/megacable|mega cable/i, 'megacable', '#0055A5'],
  [/\bsky\b/i, 'sky', '#0072C9'],
  [/\bdish\b/i, 'dish', '#E4002B'],
  [/naturgy|gas natural/i, 'naturgy', '#004571'],
  [/netflix/i, 'netflix', '#E50914'],
  [/spotify/i, 'spotify', '#1ED760'],
  [/xbox/i, 'xbox', '#107C10'],
];

/** Devuelve {url, color, texto} del logo de la compañía, o null si no hay uno guardado. */
export function logoDeMarca(...nombres) {
  const texto = nombres.filter(Boolean).join(' ');
  const m = MARCAS.find(([re]) => re.test(texto));
  return m ? { url: `/logos/${m[1]}.svg`, color: m[2], texto: m[3] ?? '#fff' } : null;
}
