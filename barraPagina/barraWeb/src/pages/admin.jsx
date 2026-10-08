import { useState, useEffect, useCallback } from "react";
import Header from "../components/header.jsx";
import Footer from "../components/footer.jsx";
import Card from "../components/card.jsx";
import Input from "../components/input.jsx";
import Button from "../components/button.jsx";
import Table from "../components/table.jsx";
import { api } from "../services/api.js";

function LoginAdmin({ onLogin }) {
  const [usuario, setUsuario] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  async function manejarSubmit(evento) {
    evento.preventDefault();
    setCargando(true);
    setError(null);

    const auth = `Basic ${btoa(`${usuario}:${password}`)}`;
    try {
      await api.admin.listarVentas(auth);
      onLogin(auth);
    } catch {
      setError("Usuario o contraseña incorrectos");
    } finally {
      setCargando(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
      <h1 className="text-2xl font-bold text-primary">Ingresar</h1>
      <Card className="mt-8">
        <form onSubmit={manejarSubmit} className="flex flex-col gap-4">
          <Input id="usuario" label="Usuario" required value={usuario} onChange={(e) => setUsuario(e.target.value)} />
          <Input
            id="password"
            label="Contraseña"
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" disabled={cargando} className="mt-2">
            {cargando ? "Ingresando…" : "Ingresar"}
          </Button>
        </form>
      </Card>
    </main>
  );
}

function PanelAdmin({ auth }) {
  const [ventas, setVentas] = useState(null);
  const [licencias, setLicencias] = useState(null);

  const cargar = useCallback(async () => {
    const [ventasData, licenciasData] = await Promise.all([
      api.admin.listarVentas(auth),
      api.admin.listarLicencias(auth),
    ]);
    setVentas(ventasData);
    setLicencias(licenciasData);
  }, [auth]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  async function manejarRevocar(id) {
    await api.admin.revocarLicencia(auth, id);
    cargar();
  }

  return (
    <main className="mx-auto w-full max-w-5xl flex-1 px-6 py-16">
      <h1 className="text-2xl font-bold text-primary">Panel de administración</h1>

      <section className="mt-10">
        <h2 className="text-lg font-bold text-primary">Ventas</h2>
        <div className="mt-4">
          <Table
            columnas={[
              { clave: "comprador_nombre", titulo: "Comprador" },
              { clave: "comprador_email", titulo: "Email" },
              { clave: "plan_nombre", titulo: "Plan" },
              { clave: "monto", titulo: "Monto" },
              { clave: "estado", titulo: "Estado" },
            ]}
            filas={ventas}
            vacio="Todavía no hay ventas"
          />
        </div>
      </section>

      <section className="mt-10">
        <h2 className="text-lg font-bold text-primary">Licencias</h2>
        <div className="mt-4">
          <Table
            columnas={[
              { clave: "codigo", titulo: "Código" },
              { clave: "comprador_nombre", titulo: "Comprador" },
              { clave: "plan_nombre", titulo: "Plan" },
              {
                clave: "activaciones",
                titulo: "Activaciones",
                render: (fila) => `${fila.activaciones_usadas}/${fila.max_activaciones}`,
              },
              { clave: "fecha_vencimiento", titulo: "Vence" },
              {
                clave: "acciones",
                titulo: "",
                render: (fila) => (
                  <Button variant="secondary" onClick={() => manejarRevocar(fila.id)}>
                    Revocar
                  </Button>
                ),
              },
            ]}
            filas={licencias}
            vacio="Todavía no hay licencias"
          />
        </div>
      </section>
    </main>
  );
}

export default function Admin() {
  const [auth, setAuth] = useState(null);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      {auth ? <PanelAdmin auth={auth} /> : <LoginAdmin onLogin={setAuth} />}
      <Footer />
    </div>
  );
}
