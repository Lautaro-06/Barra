import { Link } from "react-router-dom";

export default function Header() {
  return (
    <header className="border-b border-border bg-white">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <Link to="/" className="text-lg font-bold text-primary">
          Barra
        </Link>
        <nav className="flex items-center gap-6 text-sm font-medium text-secondary">
          <Link to="/recuperar-licencia" className="hover:text-primary">
            Recuperar licencia
          </Link>
        </nav>
      </div>
    </header>
  );
}
