import Plan from "../models/plan.js";

export async function listarPlanes(req, res) {
  const planes = await Plan.todos();
  res.json(planes);
}
