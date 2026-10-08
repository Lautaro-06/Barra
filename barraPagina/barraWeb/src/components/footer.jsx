export default function Footer() {
  return (
    <footer className="border-t border-border bg-white">
      <div className="mx-auto max-w-5xl px-6 py-6 text-sm text-muted">
        © {new Date().getFullYear()} Barra. Sistema de gestión de pedidos.
      </div>
    </footer>
  );
}
