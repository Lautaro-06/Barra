import { Routes, Route } from "react-router-dom";
import Home from "./pages/home.jsx";
import Checkout from "./pages/checkout.jsx";
import PagoExitoso from "./pages/PagoExitoso.jsx";
import PagoFallido from "./pages/PagoFallido.jsx";
import RecuperarLicencia from "./pages/RecuperarLicencia.jsx";
import Admin from "./pages/admin.jsx";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/checkout/:planId" element={<Checkout />} />
      <Route path="/pago-exitoso" element={<PagoExitoso />} />
      <Route path="/pago-fallido" element={<PagoFallido />} />
      <Route path="/recuperar-licencia" element={<RecuperarLicencia />} />
      <Route path="/admin" element={<Admin />} />
    </Routes>
  );
}
