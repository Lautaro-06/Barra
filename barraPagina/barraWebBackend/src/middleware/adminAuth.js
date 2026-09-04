import crypto from "node:crypto";

function comparacionSegura(a, b) {
  const bufA = Buffer.from(a);
  const bufB = Buffer.from(b);
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

export function adminAuth(req, res, next) {
  const header = req.headers.authorization || "";
  const [tipo, credenciales] = header.split(" ");

  if (tipo !== "Basic" || !credenciales) {
    res.set("WWW-Authenticate", 'Basic realm="Barra Admin"');
    return res.status(401).json({ error: "Autenticación requerida" });
  }

  const [usuario, password] = Buffer.from(credenciales, "base64").toString("utf8").split(":");
  const usuarioOk = process.env.ADMIN_USER || "";
  const passwordOk = process.env.ADMIN_PASSWORD || "";

  if (
    !usuarioOk ||
    !passwordOk ||
    usuario?.length !== usuarioOk.length ||
    password?.length !== passwordOk.length ||
    !comparacionSegura(usuario, usuarioOk) ||
    !comparacionSegura(password, passwordOk)
  ) {
    res.set("WWW-Authenticate", 'Basic realm="Barra Admin"');
    return res.status(401).json({ error: "Credenciales inválidas" });
  }

  next();
}
