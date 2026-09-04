import Pago from "../models/pago.js";
import Licencia from "../models/licencia.js";

export async function listarVentas(req, res) {
  const ventas = await Pago.todosConDetalle();
  res.json(ventas);
}

export async function listarLicencias(req, res) {
  const licencias = await Licencia.todasConDetalle();
  res.json(licencias);
}

export async function revocarLicencia(req, res) {
  const licencia = await Licencia.porId(req.params.id);
  if (!licencia) return res.status(404).json({ error: "Licencia inexistente" });

  await licencia.revocar();
  res.json({ ok: true });
}
