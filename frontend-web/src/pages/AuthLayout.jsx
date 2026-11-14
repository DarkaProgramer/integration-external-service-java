export default function AuthLayout({ titulo, subtitulo, children, ancho = false }) {
  return (
    <div className="auth">
      <aside className="auth-lado">
        <div className="marca marca-grande"><span className="marca-logo">H</span> Hasani Pagos</div>
        <h2>Paga tus servicios en un solo lugar.</h2>
        <p>Recargas, luz, agua, internet y más, con comprobante al instante.</p>
      </aside>
      <section className={`auth-panel ${ancho ? 'auth-panel-ancho' : ''}`}>
        <h1>{titulo}</h1>
        {subtitulo && <p className="texto-suave">{subtitulo}</p>}
        {children}
      </section>
    </div>
  );
}
