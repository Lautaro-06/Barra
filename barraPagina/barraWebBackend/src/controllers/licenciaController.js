import Licencia from "../models/licencia.js";
import Comprador from "../models/comprador.js";
import { verificarSecret } from "../utils/licenciaUtils.js";
import { enviarRecuperacion } from "../services/emailService.js";

async function buscarYValidar(codigo, secret) {
  const licencia = await Licencia.porCodigo(codigo);
  if (!licencia) return { error: "Licencia inexistente", status: 404 };

  const secretValido = await verificarSecret(secret, licencia.secretHash);
  if (!secretValido) return { error: "Credenciales inválidas", status: 401 };

  return { licencia };
}

export async function activar(req, res) {
  const { codigo, secret } = req.body || {};
  if (!codigo || !secret) return res.status(400).json({ error: "codigo y secret son requeridos" });

  const { licencia, error, status } = await buscarYValidar(codigo, secret);
  if (error) return res.status(status).json({ error });

  if (!licencia.tieneActivacionesDisponibles()) {
    return res.status(403).json({ error: "Licencia sin activaciones disponibles" });
  }

  const licenciaActivada = await licencia.registrarActivacion();
  return res.json({ valido: true, fecha_vencimiento: licenciaActivada.fechaVencimiento });
}

export async function estado(req, res) {
  const { codigo, secret } = req.query || {};
  if (!codigo || !secret) return res.status(400).json({ error: "codigo y secret son requeridos" });

  const { licencia, error, status } = await buscarYValidar(codigo, secret);
  if (error) return res.status(status).json({ error });

  return res.json({
    valido: licencia.vigente(),
    fecha_vencimiento: licencia.fechaVencimiento,
  });
}

export async function recuperar(req, res) {
  const { email } = req.body || {};
  if (!email) return res.status(400).json({ error: "email es requerido" });

  const comprador = await Comprador.porEmail(email);
  if (!comprador) return res.json({ ok: true }); // no revelamos si el email existe o no

  const licencia = await Licencia.masRecientePorComprador(comprador.id);
  if (licencia) {
    await enviarRecuperacion({ email, codigo: licencia.codigo });
  }

  return res.json({ ok: true });
}
