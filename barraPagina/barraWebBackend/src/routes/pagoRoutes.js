import { Router } from "express";
import { crearCompra, webhookPago } from "../controllers/pagoController.js";
import { asyncHandler } from "../utils/asyncHandler.js";

const router = Router();

router.post("/compras", asyncHandler(crearCompra));
router.post("/pagos/webhook", asyncHandler(webhookPago));

export default router;
