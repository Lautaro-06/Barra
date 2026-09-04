export default function Table({ columnas, filas, vacio = "Sin datos todavía" }) {
  if (!filas?.length) {
    return <p className="py-8 text-center text-sm text-muted">{vacio}</p>;
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-border">
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-border bg-surface">
            {columnas.map((columna) => (
              <th key={columna.clave} className="px-4 py-3 font-semibold text-primary">
                {columna.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((fila, indice) => (
            <tr key={fila.id ?? indice} className="border-b border-border last:border-0">
              {columnas.map((columna) => (
                <td key={columna.clave} className="px-4 py-3 text-text">
                  {columna.render ? columna.render(fila) : fila[columna.clave]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
