import { customAlphabet } from "nanoid";
import bcrypt from "bcryptjs";

const ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sin 0/O/1/I para evitar confusión al tipear
const generarSegmento = customAlphabet(ALFABETO_CODIGO, 4);
const SALT_ROUNDS = 10;

export function generarCodigo() {
  const segmentos = [generarSegmento(), generarSegmento(), generarSegmento()];
  return `BARRA-${segmentos.join("-")}`;
}

export function generarSecret() {
  const generarHex = customAlphabet("0123456789abcdef", 64);
  return generarHex();
}

export async function hashearSecret(secret) {
  return bcrypt.hash(secret, SALT_ROUNDS);
}

export async function verificarSecret(secret, hash) {
  return bcrypt.compare(secret, hash);
}
