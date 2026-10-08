import { Router } from "express";
import { listarPlanes } from "../controllers/planController.js";
import { asyncHandler } from "../utils/asyncHandler.js";

const router = Router();

router.get("/planes", asyncHandler(listarPlanes));

export default router;
