import "dotenv/config";
import express from "express";
import cors from "cors";
import { initDb } from "./db.js";
import planRoutes from "./routes/planRoutes.js";
import pagoRoutes from "./routes/pagoRoutes.js";
import licenciaRoutes from "./routes/licenciaRoutes.js";
import adminRoutes from "./routes/adminRoutes.js";

const app = express();

app.use(cors());
app.use(express.json());

app.get("/api/health", (req, res) => res.json({ ok: true }));

app.use("/api", planRoutes);
app.use("/api", pagoRoutes);
app.use("/api", licenciaRoutes);
app.use("/api/admin", adminRoutes);

app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: "Error interno" });
});

const PORT = process.env.PORT || 4000;

initDb()
  .then(() => {
    app.listen(PORT, () => console.log(`barraWebBackend escuchando en http://localhost:${PORT}`));
  })
  .catch((error) => {
    console.error("No se pudo inicializar la base de datos:", error);
    process.exit(1);
  });

export default app;
