import { useState } from "react";
import Header from "../components/header.jsx";
import Footer from "../components/footer.jsx";
import Card from "../components/card.jsx";
import Input from "../components/input.jsx";
import Button from "../components/button.jsx";
import { api } from "../services/api.js";

export default function RecuperarLicencia() {
  const [email, setEmail] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [enviado, setEnviado] = useState(false);

  async function manejarSubmit(evento) {
    evento.preventDefault();
    setEnviando(true);
    await api.recuperarLicencia(email).catch(() => {});
    setEnviando(false);
    setEnviado(true);
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Header />

      <main className="mx-auto w-full max-w-md flex-1 px-6 py-16">
        <h1 className="text-2xl font-bold text-primary">Recuperar mi licencia</h1>
        <p className="mt-2 text-sm text-muted">
          Ingresá el email con el que compraste y te mandamos tu código de licencia.
        </p>

        <Card className="mt-8">
          {enviado ? (
            <p className="text-sm text-text">
              Si el email está registrado, te llega un mail con tu código en los próximos minutos.
            </p>
          ) : (
            <form onSubmit={manejarSubmit} className="flex flex-col gap-4">
              <Input
                id="email"
                label="Email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <Button type="submit" disabled={enviando} className="mt-2">
                {enviando ? "Enviando…" : "Recuperar licencia"}
              </Button>
            </form>
          )}
        </Card>
      </main>

      <Footer />
    </div>
  );
}
