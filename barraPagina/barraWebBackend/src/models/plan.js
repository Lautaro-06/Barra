import pool from "../db.js";

export default class Plan {
  constructor({ id, nombre, precio_ars, dias_renovacion, max_activaciones, disponible }) {
    this.id = id;
    this.nombre = nombre;
    this.precioArs = Number(precio_ars);
    this.diasRenovacion = dias_renovacion;
    this.maxActivaciones = max_activaciones;
    this.disponible = Boolean(disponible);
  }

  esGratuito() {
    return this.precioArs === 0;
  }

  static async todos() {
    const [rows] = await pool.query("SELECT * FROM planes ORDER BY precio_ars ASC");
    return rows.map((row) => new Plan(row));
  }

  static async porId(id) {
    const [rows] = await pool.query("SELECT * FROM planes WHERE id = ?", [id]);
    return rows[0] ? new Plan(rows[0]) : null;
  }
}
