import Plan from "../models/plan.js";
import Comprador from "../models/comprador.js";
import Licencia from "../models/licencia.js";
import Pago from "../models/pago.js";
import { generarCodigo, generarSecret, hashearSecret } from "../utils/licenciaUtils.js";
import { enviarLicencia } from "../services/emailService.js";
import { crearPreferencia, obtenerPago, verificarFirmaWebhook } from "../services/mercadoPagoService.js";

async function emitirLicencia({ comprador, plan }) {
  const codigo = generarCodigo();
  const secret = generarSecret();
  const secretHash = await hashearSecret(secret);

  const licencia = await Licencia.crear({ compradorId: comprador.id, plan, codigo, secretHash });
  await enviarLicencia({ email: comprador.email, nombre: comprador.nombre, plan, codigo, secret });

  return licencia;
}

export async function crearCompra(req, res) {
  const { plan_id, comprador } = req.body || {};
  if (!plan_id || !comprador?.nombre || !comprador?.email) {
    return res.status(400).json({ error: "plan_id, comprador.nombre y comprador.email son requeridos" });
  }

  const plan = await Plan.porId(plan_id);
  if (!plan) return res.status(404).json({ error: "Plan inexistente" });
  if (!plan.disponible) return res.status(409).json({ error: "Plan no disponible" });

  const compradorCreado = await Comprador.crear(comprador);

  if (plan.esGratuito()) {
    const licencia = await emitirLicencia({ comprador: compradorCreado, plan });
    await Pago.crear({
      licenciaId: licencia.id,
      planId: plan.id,
      compradorId: compradorCreado.id,
      monto: 0,
      estado: "gratuito",
    });
    return res.status(201).json({ redirect: "/pago-exitoso" });
  }

  const pago = await Pago.crear({
    planId: plan.id,
    compradorId: compradorCreado.id,
    monto: plan.precioArs,
    estado: "pendiente",
  });

  const initPoint = await crearPreferencia({ pagoId: pago.id, plan, comprador: compradorCreado });
  return res.status(201).json({ redirect: initPoint });
}

export async function webhookPago(req, res) {
  // Siempre 200 rápido: si tardamos o devolvemos error, MP reintenta agresivamente.
  const dataId = req.query["data.id"] || req.body?.data?.id;
  const xSignature = req.headers["x-signature"];
  const xRequestId = req.headers["x-request-id"];

  if (!dataId) return res.sendStatus(200);

  const firmaValida = verificarFirmaWebhook({ xSignature, xRequestId, dataId });
  if (!firmaValida) {
    console.warn("Webhook de Mercado Pago con firma inválida, ignorado");
    return res.sendStatus(200);
  }

  try {
    const pagoMp = await obtenerPago(dataId);
    const pagoId = Number(pagoMp.external_reference);
    const pago = await Pago.porId(pagoId);
    if (!pago) return res.sendStatus(200);

    if (pagoMp.status === "approved" && pago.estado !== "aprobado") {
      const plan = await Plan.porId(pago.planId);
      const comprador = await Comprador.porId(pago.compradorId);

      const licencia = await emitirLicencia({ comprador, plan });
      await pago.actualizarEstado("aprobado", { licenciaId: licencia.id, mpPaymentId: String(pagoMp.id) });
    } else if (pagoMp.status === "rejected") {
      await pago.actualizarEstado("rechazado", { mpPaymentId: String(pagoMp.id) });
    }
  } catch (error) {
    console.error("Error procesando webhook de Mercado Pago:", error);
  }

  return res.sendStatus(200);
}
