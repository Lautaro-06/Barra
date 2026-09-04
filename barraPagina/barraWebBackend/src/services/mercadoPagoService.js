import crypto from "node:crypto";
import { MercadoPagoConfig, Preference, Payment } from "mercadopago";

function getClient() {
  const accessToken = process.env.MP_ACCESS_TOKEN;
  if (!accessToken) throw new Error("Falta MP_ACCESS_TOKEN en el .env");
  return new MercadoPagoConfig({ accessToken });
}

export async function crearPreferencia({ pagoId, plan, comprador }) {
  const preference = new Preference(getClient());
  const baseUrl = process.env.PUBLIC_URL || "http://localhost:5173";

  const respuesta = await preference.create({
    body: {
      items: [
        {
          title: `Licencia Barra - Plan ${plan.nombre}`,
          quantity: 1,
          unit_price: plan.precioArs,
          currency_id: "ARS",
        },
      ],
      payer: { name: comprador.nombre, email: comprador.email },
      external_reference: String(pagoId),
      back_urls: {
        success: `${baseUrl}/pago-exitoso`,
        failure: `${baseUrl}/pago-fallido`,
        pending: `${baseUrl}/pago-fallido`,
      },
      auto_return: "approved",
    },
  });

  return respuesta.init_point;
}

export async function obtenerPago(mpPaymentId) {
  const payment = new Payment(getClient());
  return payment.get({ id: mpPaymentId });
}

// Valida la firma x-signature que manda Mercado Pago en cada webhook.
// Referencia: https://www.mercadopago.com.ar/developers/es/docs/your-integrations/notifications/webhooks
export function verificarFirmaWebhook({ xSignature, xRequestId, dataId }) {
  const secret = process.env.MP_WEBHOOK_SECRET;
  if (!secret) {
    console.warn("MP_WEBHOOK_SECRET no configurado — se acepta el webhook sin validar firma (solo dev).");
    return true;
  }
  if (!xSignature) return false;

  const partes = Object.fromEntries(
    xSignature.split(",").map((parte) => {
      const [clave, valor] = parte.split("=");
      return [clave?.trim(), valor?.trim()];
    })
  );
  const { ts, v1 } = partes;
  if (!ts || !v1) return false;

  const manifest = `id:${dataId};request-id:${xRequestId};ts:${ts};`;
  const firmaEsperada = crypto.createHmac("sha256", secret).update(manifest).digest("hex");

  return crypto.timingSafeEqual(Buffer.from(firmaEsperada), Buffer.from(v1));
}
