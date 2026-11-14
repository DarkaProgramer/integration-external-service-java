import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { RutaPrivada, RutaPublica } from './components/ProtectedRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import CatalogPage from './pages/CatalogPage';
import PayPage from './pages/PayPage';
import HistoryPage from './pages/HistoryPage';
import PaymentDetailPage from './pages/PaymentDetailPage';
import AccountPage from './pages/AccountPage';
import NotFoundPage from './pages/NotFoundPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<RutaPublica />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/registro" element={<RegisterPage />} />
          </Route>
          <Route element={<RutaPrivada />}>
            <Route element={<Layout />}>
              <Route index element={<CatalogPage />} />
              <Route path="/pagar/:identificador" element={<PayPage />} />
              <Route path="/historial" element={<HistoryPage />} />
              <Route path="/historial/:folio" element={<PaymentDetailPage />} />
              <Route path="/cuenta" element={<AccountPage />} />
            </Route>
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
