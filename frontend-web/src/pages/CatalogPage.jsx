import { useMemo, useState } from 'react';
import { catalogoApi } from '../api/catalogoApi';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../auth/AuthContext';
import PageHeader from '../components/PageHeader';
import ProductCard from '../components/ProductCard';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';
import { ErrorApi } from '../components/Alert';

const TODAS = 'Todas';

export default function CatalogPage() {
  const { perfil } = useAuth();
  const [forzar, setForzar] = useState(0);
  const { datos: productos, cargando, error, recargar } = useAsync(() => catalogoApi.productos({ forzar: forzar > 0 }), [forzar]);
  const [busqueda, setBusqueda] = useState('');
  const [categoria, setCategoria] = useState(TODAS);

  const categorias = useMemo(
    () => [TODAS, ...new Set((productos ?? []).map((p) => p.categoria).filter(Boolean))].sort((a, b) => (a === TODAS ? -1 : b === TODAS ? 1 : a.localeCompare(b))),
    [productos],
  );
  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return (productos ?? []).filter(
      (p) => (categoria === TODAS || p.categoria === categoria)
        && (!q || [p.nombre, p.servicio, p.descripcion, p.categoria].some((t) => t?.toLowerCase().includes(q))),
    );
  }, [productos, busqueda, categoria]);

  return (
    <>
      <PageHeader
        titulo={`Hola${perfil?.nombre ? `, ${perfil.nombre}` : ''}`}
        subtitulo="Elige el servicio que quieres pagar."
        accion={<Button variante="secundario" onClick={() => setForzar((f) => f + 1)} cargando={cargando && !!productos}>Actualizar</Button>}
      />
      <div className="filtros">
        <input className="buscador" type="search" placeholder="Buscar servicio, compañía o categoría…" value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)} aria-label="Buscar en el catálogo" />
        <div className="chips" role="tablist" aria-label="Categorías">
          {categorias.map((c) => (
            <button key={c} role="tab" aria-selected={categoria === c} className={`chip ${categoria === c ? 'chip-activo' : ''}`} onClick={() => setCategoria(c)}>{c}</button>
          ))}
        </div>
      </div>
      {error && <ErrorApi error={error} onReintentar={recargar} />}
      {cargando && !productos && <div className="rejilla-productos">{Array.from({ length: 8 }, (_, i) => <div key={i} className="tarjeta esqueleto" />)}</div>}
      {productos && filtrados.length === 0 && (
        <EmptyState icono="🔍" titulo="Sin resultados">Prueba con otra búsqueda o categoría.</EmptyState>
      )}
      {productos && filtrados.length > 0 && (
        <div className="rejilla-productos">
          {filtrados.map((p) => <ProductCard key={p.identificador} producto={p} />)}
        </div>
      )}
      {cargando && productos && <Spinner texto="Actualizando catálogo…" />}
    </>
  );
}
