import { Router } from "express";
import { adminAuth } from "../middleware/adminAuth.js";
import { listarVentas, listarLicencias, revocarLicencia } from "../controllers/adminController.js";
import { asyncHandler } from "../utils/asyncHandler.js";

const router = Router();

router.use(adminAuth);
router.get("/ventas", asyncHandler(listarVentas));
router.get("/licencias", asyncHandler(listarLicencias));
router.post("/licencias/:id/revocar", asyncHandler(revocarLicencia));

export default router;
