import { Link } from "react-router-dom";
import Header from "../components/header.jsx";
import Footer from "../components/footer.jsx";
import Card from "../components/card.jsx";

export default function PagoFallido() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />

      <main className="mx-auto w-full max-w-md flex-1 px-6 py-16">
        <Card className="text-center">
          <h1 className="text-2xl font-bold text-primary">El pago no se pudo procesar</h1>
          <p className="mt-3 text-sm text-muted">
            No te preocupes, no se generó ningún cargo. Podés intentarlo de nuevo.
          </p>
          <Link to="/" className="mt-6 inline-block text-sm font-semibold text-cta hover:text-cta-hover">
            Volver al inicio
          </Link>
        </Card>
      </main>

      <Footer />
    </div>
  );
}
