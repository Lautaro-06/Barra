import { useEffect, useState } from "react";
import Header from "../components/header.jsx";
import Footer from "../components/footer.jsx";
import PlanCard from "../components/PlanCard.jsx";
import { api } from "../services/api.js";

export default function Home() {
  const [planes, setPlanes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    api
      .listarPlanes()
      .then(setPlanes)
      .catch((err) => setError(err.message))
      .finally(() => setCargando(false));
  }, []);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />

      <main className="mx-auto w-full max-w-5xl flex-1 px-6 py-16">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-4xl font-bold text-primary">
            Gestioná los pedidos de tu local sin complicarte
          </h1>
          <p className="mt-4 text-lg text-muted">
            Barra es el sistema de gestión de pedidos para rotiserías, dietéticas y cafés.
            Cajero, cocina y stock, todo en un solo lugar.
          </p>
        </div>

        <section className="mt-16">
          <h2 className="text-center text-2xl font-bold text-primary">Planes</h2>

          {cargando && <p className="mt-8 text-center text-muted">Cargando planes…</p>}
          {error && <p className="mt-8 text-center text-danger">No se pudieron cargar los planes: {error}</p>}

          {!cargando && !error && (
            <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-3">
              {planes.map((plan) => (
                <PlanCard key={plan.id} plan={plan} />
              ))}
            </div>
          )}
        </section>
      </main>

      <Footer />
    </div>
  );
}
