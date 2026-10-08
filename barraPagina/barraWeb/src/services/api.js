const API_URL = import.meta.env.VITE_API_URL || "http://localhost:4000/api";

async function pedido(path, options = {}) {
  const respuesta = await fetch(`${API_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...options.headers },
    ...options,
  });

  const data = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    throw new Error(data?.error || `Error ${respuesta.status}`);
  }
  return data;
}

export const api = {
  listarPlanes: () => pedido("/planes"),

  crearCompra: (planId, comprador) =>
    pedido("/compras", {
      method: "POST",
      body: JSON.stringify({ plan_id: planId, comprador }),
    }),

  recuperarLicencia: (email) =>
    pedido("/licencias/recuperar", {
      method: "POST",
      body: JSON.stringify({ email }),
    }),

  admin: {
    listarVentas: (auth) => pedido("/admin/ventas", { headers: { Authorization: auth } }),
    listarLicencias: (auth) => pedido("/admin/licencias", { headers: { Authorization: auth } }),
    revocarLicencia: (auth, id) =>
      pedido(`/admin/licencias/${id}/revocar`, { method: "POST", headers: { Authorization: auth } }),
  },
};
