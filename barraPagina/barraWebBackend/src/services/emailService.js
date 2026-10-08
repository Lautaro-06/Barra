import nodemailer from "nodemailer";

let transporter;

function getTransporter() {
  if (transporter) return transporter;

  if (!process.env.SMTP_HOST) {
    // Sin config de SMTP: logueamos el mail en consola en vez de fallar.
    // Útil para desarrollo local sin credenciales reales.
    transporter = {
      sendMail: async (opciones) => {
        console.log("--- EMAIL (SMTP no configurado, solo consola) ---");
        console.log(`Para: ${opciones.to}`);
        console.log(`Asunto: ${opciones.subject}`);
        console.log(opciones.text);
        console.log("---------------------------------------------------");
      },
    };
    return transporter;
  }

  transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: Number(process.env.SMTP_PORT || 587),
    secure: process.env.SMTP_SECURE === "true",
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS,
    },
  });
  return transporter;
}

export async function enviarLicencia({ email, nombre, plan, codigo, secret }) {
  const descargaUrl = process.env.DOWNLOAD_URL || "https://barra.example.com/descargar";

  await getTransporter().sendMail({
    from: process.env.SMTP_FROM || "no-reply@barra.example.com",
    to: email,
    subject: "Tu licencia de Barra",
    text: [
      `Hola ${nombre},`,
      "",
      `Gracias por elegir el plan ${plan.nombre}.`,
      "",
      `Código de licencia: ${codigo}`,
      `Clave secreta: ${secret}`,
      "",
      "Guardá estos dos datos: los vas a necesitar para activar Barra la primera vez que la abras.",
      "No los compartas ni los reenvíes.",
      "",
      `Descargá el instalador acá: ${descargaUrl}`,
    ].join("\n"),
  });
}

export async function enviarRecuperacion({ email, codigo }) {
  await getTransporter().sendMail({
    from: process.env.SMTP_FROM || "no-reply@barra.example.com",
    to: email,
    subject: "Tu código de licencia de Barra",
    text: [
      `Tu código de licencia es: ${codigo}`,
      "",
      "Por seguridad no reenviamos la clave secreta por este medio. Si la perdiste, contactanos.",
    ].join("\n"),
  });
}
