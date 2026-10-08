import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Header from "../components/header.jsx";
import Footer from "../components/footer.jsx";
import Card from "../components/card.jsx";
import Input from "../components/input.jsx";
import Button from "../components/button.jsx";
import { api } from "../services/api.js";

export default function Checkout() {
  const { planId } = useParams();
  const navigate = useNavigate();

  const [nombre, setNombre] = useState("");
  const [email, setEmail] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  async function manejarSubmit(evento) {
    evento.preventDefault();
    setEnviando(true);
    setError(null);

    try {
      const resultado = await api.crearCompra(planId, { nombre, email });

      if (resultado.redirect.startsWith("/")) {
        navigate(resultado.redirect);
      } else {
        window.location.href = resultado.redirect;
      }
    } catch (err) {
      setError(err.message);
      setEnviando(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Header />

      <main className="mx-auto w-full max-w-md flex-1 px-6 py-16">
        <h1 className="text-2xl font-bold text-primary">Completá tus datos</h1>
        <p className="mt-2 text-sm text-muted">
          Te vamos a mandar tu código de licencia a este email.
        </p>

        <Card className="mt-8">
          <form onSubmit={manejarSubmit} className="flex flex-col gap-4">
            <Input
              id="nombre"
              label="Nombre"
              required
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
            />
            <Input
              id="email"
              label="Email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />

            {error && <p className="text-sm text-danger">{error}</p>}

            <Button type="submit" disabled={enviando} className="mt-2">
              {enviando ? "Procesando…" : "Continuar"}
            </Button>
          </form>
        </Card>
      </main>

      <Footer />
    </div>
  );
}
