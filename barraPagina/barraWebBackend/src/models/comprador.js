import pool from "../db.js";

export default class Comprador {
  constructor({ id, nombre, email, creado_en }) {
    this.id = id;
    this.nombre = nombre;
    this.email = email;
    this.creadoEn = creado_en;
  }

  static async crear({ nombre, email }) {
    const [result] = await pool.query(
      "INSERT INTO compradores (nombre, email) VALUES (?, ?)",
      [nombre, email]
    );
    return new Comprador({ id: result.insertId, nombre, email });
  }

  static async porId(id) {
    const [rows] = await pool.query("SELECT * FROM compradores WHERE id = ?", [id]);
    return rows[0] ? new Comprador(rows[0]) : null;
  }

  static async porEmail(email) {
    const [rows] = await pool.query("SELECT * FROM compradores WHERE email = ? ORDER BY id DESC LIMIT 1", [email]);
    return rows[0] ? new Comprador(rows[0]) : null;
  }
}
