import { Link } from "react-router-dom";
import Card from "./card.jsx";
import Button from "./button.jsx";

export default function PlanCard({ plan }) {
  const precio = plan.precioArs === 0 ? "Gratis" : `$${plan.precioArs} ARS`;

  return (
    <Card className="flex flex-col gap-4">
      <div>
        <h3 className="text-lg font-bold text-primary">{plan.nombre}</h3>
        <p className="mt-1 text-2xl font-bold text-text">{precio}</p>
        <p className="mt-1 text-sm text-muted">
          Licencia válida por {plan.diasRenovacion} días
        </p>
      </div>

      {plan.disponible ? (
        <Button as={Link} to={`/checkout/${plan.id}`} className="mt-auto">
          Elegir plan
        </Button>
      ) : (
        <Button variant="disabled" disabled className="mt-auto">
          No disponible
        </Button>
      )}
    </Card>
  );
}
