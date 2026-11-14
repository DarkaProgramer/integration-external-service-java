export default function PageHeader({ titulo, subtitulo, accion }) {
  return (
    <div className="pagina-cabecera">
      <div>
        <h1>{titulo}</h1>
        {subtitulo && <p className="texto-suave">{subtitulo}</p>}
      </div>
      {accion}
    </div>
  );
}
