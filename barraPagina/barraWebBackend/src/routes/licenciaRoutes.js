import { Router } from "express";
import { activar, estado, recuperar } from "../controllers/licenciaController.js";
import { asyncHandler } from "../utils/asyncHandler.js";

const router = Router();

router.post("/licencias/activar", asyncHandler(activar));
router.get("/licencias/estado", asyncHandler(estado));
router.post("/licencias/recuperar", asyncHandler(recuperar));

export default router;
