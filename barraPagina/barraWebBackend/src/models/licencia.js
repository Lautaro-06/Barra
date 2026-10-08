import pool from "../db.js";

export default class Licencia {
  constructor({
    id,
    comprador_id,
    plan_id,
    codigo,
    secret_hash,
    dias_renovacion,
    activaciones_usadas,
    max_activaciones,
    fecha_activacion,
    fecha_vencimiento,
    creado_en,
  }) {
    this.id = id;
    this.compradorId = comprador_id;
    this.planId = plan_id;
    this.codigo = codigo;
    this.secretHash = secret_hash;
    this.diasRenovacion = dias_renovacion;
    this.activacionesUsadas = activaciones_usadas;
    this.maxActivaciones = max_activaciones;
    this.fechaActivacion = fecha_activacion;
    this.fechaVencimiento = fecha_vencimiento;
    this.creadoEn = creado_en;
  }

  tieneActivacionesDisponibles() {
    return this.activacionesUsadas < this.maxActivaciones;
  }

  vigente() {
    if (this.activacionesUsadas > this.maxActivaciones) return false; // revocada por admin
    if (!this.fechaVencimiento) return true; // no activada todavía
    return new Date(this.fechaVencimiento) > new Date();
  }

  static async crear({ compradorId, plan, codigo, secretHash }) {
    const [result] = await pool.query(
      `INSERT INTO licencias
        (comprador_id, plan_id, codigo, secret_hash, dias_renovacion, max_activaciones)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [compradorId, plan.id, codigo, secretHash, plan.diasRenovacion, plan.maxActivaciones]
    );
    return Licencia.porId(result.insertId);
  }

  static async porId(id) {
    const [rows] = await pool.query("SELECT * FROM licencias WHERE id = ?", [id]);
    return rows[0] ? new Licencia(rows[0]) : null;
  }

  static async porCodigo(codigo) {
    const [rows] = await pool.query("SELECT * FROM licencias WHERE codigo = ?", [codigo]);
    return rows[0] ? new Licencia(rows[0]) : null;
  }

  static async masRecientePorComprador(compradorId) {
    const [rows] = await pool.query(
      "SELECT * FROM licencias WHERE comprador_id = ? ORDER BY id DESC LIMIT 1",
      [compradorId]
    );
    return rows[0] ? new Licencia(rows[0]) : null;
  }

  async registrarActivacion() {
    const vencimiento = new Date();
    vencimiento.setDate(vencimiento.getDate() + this.diasRenovacion);

    await pool.query(
      `UPDATE licencias
       SET activaciones_usadas = activaciones_usadas + 1,
           fecha_activacion = COALESCE(fecha_activacion, NOW()),
           fecha_vencimiento = COALESCE(fecha_vencimiento, ?)
       WHERE id = ?`,
      [vencimiento, this.id]
    );
    return Licencia.porId(this.id);
  }

  static async todasConDetalle() {
    const [rows] = await pool.query(`
      SELECT
        l.id, l.comprador_id, l.plan_id, l.codigo, l.dias_renovacion,
        l.activaciones_usadas, l.max_activaciones, l.fecha_activacion,
        l.fecha_vencimiento, l.creado_en,
        c.nombre AS comprador_nombre, c.email AS comprador_email, pl.nombre AS plan_nombre
      FROM licencias l
      LEFT JOIN compradores c ON c.id = l.comprador_id
      LEFT JOIN planes pl ON pl.id = l.plan_id
      ORDER BY l.id DESC
    `);
    return rows;
  }

  async revocar() {
    await pool.query("UPDATE licencias SET max_activaciones = 0 WHERE id = ?", [this.id]);
    this.maxActivaciones = 0;
  }
}
