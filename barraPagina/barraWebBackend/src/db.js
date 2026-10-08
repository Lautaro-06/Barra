import mysql from "mysql2/promise";

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "barra_web",
  waitForConnections: true,
  connectionLimit: 10,
});

const SCHEMA = `
CREATE TABLE IF NOT EXISTS planes (
  id INT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(20) NOT NULL,
  precio_ars DECIMAL(10,2) NOT NULL DEFAULT 0,
  dias_renovacion INT NOT NULL,
  max_activaciones INT NOT NULL DEFAULT 1,
  disponible BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS compradores (
  id INT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  creado_en DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS licencias (
  id INT PRIMARY KEY AUTO_INCREMENT,
  comprador_id INT NOT NULL,
  plan_id INT NOT NULL,
  codigo VARCHAR(32) UNIQUE NOT NULL,
  secret_hash VARCHAR(60) NOT NULL,
  dias_renovacion INT NOT NULL,
  activaciones_usadas INT NOT NULL DEFAULT 0,
  max_activaciones INT NOT NULL,
  fecha_activacion DATETIME NULL,
  fecha_vencimiento DATETIME NULL,
  creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (comprador_id) REFERENCES compradores(id),
  FOREIGN KEY (plan_id) REFERENCES planes(id)
);

CREATE TABLE IF NOT EXISTS pagos (
  id INT PRIMARY KEY AUTO_INCREMENT,
  licencia_id INT NULL,
  plan_id INT NULL,
  comprador_id INT NULL,
  mp_payment_id VARCHAR(64) NULL,
  monto DECIMAL(10,2) NOT NULL,
  estado ENUM('pendiente','aprobado','rechazado','gratuito') NOT NULL,
  creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (licencia_id) REFERENCES licencias(id),
  FOREIGN KEY (plan_id) REFERENCES planes(id),
  FOREIGN KEY (comprador_id) REFERENCES compradores(id)
);
`;

const PLANES_SEED = [
  { nombre: "Gratis", precio_ars: 0, dias_renovacion: 10, max_activaciones: 1, disponible: true },
  { nombre: "Pro", precio_ars: 100, dias_renovacion: 30, max_activaciones: 1, disponible: false },
  { nombre: "Max", precio_ars: 150, dias_renovacion: 365, max_activaciones: 1, disponible: false },
];

export async function initDb() {
  const statements = SCHEMA.split(";").map((s) => s.trim()).filter(Boolean);
  for (const statement of statements) {
    await pool.query(statement);
  }

  const [rows] = await pool.query("SELECT COUNT(*) AS total FROM planes");
  if (rows[0].total === 0) {
    for (const plan of PLANES_SEED) {
      await pool.query(
        "INSERT INTO planes (nombre, precio_ars, dias_renovacion, max_activaciones, disponible) VALUES (?, ?, ?, ?, ?)",
        [plan.nombre, plan.precio_ars, plan.dias_renovacion, plan.max_activaciones, plan.disponible]
      );
    }
  }
}

export default pool;
