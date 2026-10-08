import pool from "../db.js";

export default class Pago {
  constructor({ id, licencia_id, plan_id, comprador_id, mp_payment_id, monto, estado, creado_en }) {
    this.id = id;
    this.licenciaId = licencia_id;
    this.planId = plan_id;
    this.compradorId = comprador_id;
    this.mpPaymentId = mp_payment_id;
    this.monto = Number(monto);
    this.estado = estado;
    this.creadoEn = creado_en;
  }

  static async crear({ licenciaId = null, planId = null, compradorId = null, mpPaymentId = null, monto, estado }) {
    const [result] = await pool.query(
      "INSERT INTO pagos (licencia_id, plan_id, comprador_id, mp_payment_id, monto, estado) VALUES (?, ?, ?, ?, ?, ?)",
      [licenciaId, planId, compradorId, mpPaymentId, monto, estado]
    );
    return Pago.porId(result.insertId);
  }

  static async porId(id) {
    const [rows] = await pool.query("SELECT * FROM pagos WHERE id = ?", [id]);
    return rows[0] ? new Pago(rows[0]) : null;
  }

  static async todosConDetalle() {
    const [rows] = await pool.query(`
      SELECT p.*, c.nombre AS comprador_nombre, c.email AS comprador_email, pl.nombre AS plan_nombre
      FROM pagos p
      LEFT JOIN compradores c ON c.id = p.comprador_id
      LEFT JOIN planes pl ON pl.id = p.plan_id
      ORDER BY p.id DESC
    `);
    return rows;
  }

  static async porMpPaymentId(mpPaymentId) {
    const [rows] = await pool.query("SELECT * FROM pagos WHERE mp_payment_id = ?", [mpPaymentId]);
    return rows[0] ? new Pago(rows[0]) : null;
  }

  async actualizarEstado(estado, { licenciaId = null, mpPaymentId = null } = {}) {
    await pool.query(
      "UPDATE pagos SET estado = ?, licencia_id = COALESCE(?, licencia_id), mp_payment_id = COALESCE(?, mp_payment_id) WHERE id = ?",
      [estado, licenciaId, mpPaymentId, this.id]
    );
    this.estado = estado;
    if (licenciaId) this.licenciaId = licenciaId;
    if (mpPaymentId) this.mpPaymentId = mpPaymentId;
  }
}
