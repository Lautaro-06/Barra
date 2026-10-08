# Presupuesto inicial — Barra

| | |
|---|---|
| Fecha | 8 de octubre de 2026 |
| Moneda | Pesos argentinos (ARS), salvo que se indique otra |
| Tipo de cambio usado | Dólar tarjeta para servicios digitales: **$2.002** (06/10/2026), porque el hosting se paga con tarjeta en dólares |
| Vigencia | 30 días: con la inflación, los valores en pesos hay que revisarlos seguido |

Los valores marcados como **supuesto** son decisiones del equipo o estimaciones sin una fuente
exacta; están señalados para que se puedan ajustar. Las fuentes están al final.

---

## 1. Costo de desarrollo

Aunque el trabajo es propio, ponerle un valor a la hora sirve para justificar el precio de venta.

**Valor hora de referencia: $7.000** (supuesto). Se toma el punto medio del rango de sueldo de un
desarrollador junior en Argentina a mediados de 2026 (ARS 800.000 a 1.400.000 por mes) dividido
160 horas mensuales: $1.100.000 / 160 ≈ $6.875, redondeado a $7.000.

**Horas:** cada día de tarea del Gantt se estima en 4 horas de trabajo efectivo (el equipo trabaja
a tiempo parcial). Se suman dos tareas que no estaban en el plan y que se desarrollaron.

| Tarea | Días | Horas | Costo |
|---|---|---|---|
| A – Relevamiento | 3 | 12 | $84.000 |
| B – Actores, alcance y requisitos | 2 | 8 | $56.000 |
| C – Casos de uso | 2 | 8 | $56.000 |
| D – DER | 2 | 8 | $56.000 |
| E – Elección de tecnologías | 1 | 4 | $28.000 |
| F – Diseño de arquitectura | 2 | 8 | $56.000 |
| G – Backend Python | 8 | 32 | $224.000 |
| H – GUI de escritorio | 8 | 32 | $224.000 |
| I – Web de venta | 6 | 24 | $168.000 |
| J – Integración app de escritorio | 3 | 12 | $84.000 |
| K – Integración web de venta | 2 | 8 | $56.000 |
| L – Empaquetado e instalador | 2 | 8 | $56.000 |
| M – Pruebas integrales | 4 | 16 | $112.000 |
| N – Manual de usuario | 2 | 8 | $56.000 |
| O – Presupuesto inicial | 1 | 4 | $28.000 |
| P – Propuesta formal | 2 | 8 | $56.000 |
| Q – FODA y estado del arte | 2 | 8 | $56.000 |
| R – Presentación | 2 | 8 | $56.000 |
| S – Entrega y defensa | 1 | 4 | $28.000 |
| *Fuera del plan:* mesas, cuentas y ticket | 4 | 16 | $112.000 |
| *Fuera del plan:* configuración de email y resumen diario | 8 | 32 | $224.000 |
| **Subtotal** | **67** | **268** | **$1.876.000** |
| Contingencia (10 %) | | | $187.600 |
| **Total de desarrollo** | | | **$2.063.600** |

> Si el equipo registra las horas reales, conviene reemplazar la columna «Horas» por las reales:
> el avance real (Gantt §11.2 del documento del proyecto) duró 8 semanas.

---

## 2. Costos fijos (mensuales)

| Concepto | Detalle | En origen | Por mes (ARS) |
|---|---|---|---|
| Servidor (VPS) | DigitalOcean Droplet básico de 1 GB de RAM: corre la web, el backend Node y MySQL | US$ 6 | $12.012 |
| Backups del servidor | Copias semanales del Droplet (20 % del precio) | US$ 1,20 | $2.402 |
| Dominio `.com.ar` | NIC Argentina, renovación anual. **Supuesto:** $10.000 por año (verificar el arancel vigente en nic.ar) | $10.000 / año | $833 |
| Certificado SSL | Let's Encrypt | Gratis | $0 |
| Email transaccional | Envío de licencias con una cuenta SMTP en plan gratuito (volumen bajo) | Gratis | $0 |
| Herramientas de desarrollo | Java, Python, SQLite, Node, React, MySQL Community, Maven, PyInstaller: software libre | Gratis | $0 |
| **Total fijo** | | | **≈ $15.248** |

La app de escritorio no tiene costo de servidor: corre en la PC de cada local.

---

## 3. Costos variables (por venta)

| Concepto | Valor |
|---|---|
| Comisión de Mercado Pago con tarjeta de crédito y acreditación inmediata | 6,49 % + IVA (21 %) = **7,85 %** efectivo |
| Alternativa: acreditación más lenta | Baja la comisión; la página oficial informa desde 5,99 % + IVA al instante y 4,19 % + IVA a 10 días |
| Plan Gratis | Sin comisión (no pasa por Mercado Pago) |

Se usa el valor más alto encontrado (7,85 %) para no sobreestimar el margen.

---

## 4. Precio de venta

### 4.1 Referencia de mercado

| Competidor | Precio mensual (Argentina) |
|---|---|
| Fudo Inicial | $22.500 |
| Fudo Avanzado | $43.900 |
| Fudo Pro | $69.500 |
| HivePOS, Pedix | No publican precios |

Barra es más simple que Fudo (sin pedidos online ni funciones de cara al comensal), así que se
posiciona por debajo de su plan más barato.

### 4.2 Pago único vs. suscripción

| | Pago único | Suscripción por período (recomendada) |
|---|---|---|
| Ingreso | Uno solo por local | Recurrente |
| Soporte y actualizaciones | Hay que cobrarlos aparte | Incluidos mientras el plan esté vigente |
| Encaje con el sistema | Habría que cambiar la lógica de licencias | Ya implementado: cada plan tiene `dias_renovacion` |
| Barrera de entrada para el local | Alta (un monto grande de una vez) | Baja (prueba gratis + mensual) |

**Recomendación:** suscripción por período, como ya modela la web.

### 4.3 Precios propuestos

| Plan | Duración | Precio propuesto | Comisión MP (7,85 %) | Neto |
|---|---|---|---|---|
| Gratis (prueba) | 10 días | $0 | $0 | $0 |
| Pro | 30 días | **$12.000** | $942 | $11.058 |
| Max | 365 días | **$120.000** (equivale a 10 meses: 2 gratis) | $9.423 | $110.577 |

> Hoy la web tiene cargados $100 (Pro) y $150 (Max) como **precios de prueba** y los planes pagos
> deshabilitados. Para salir a vender hay que actualizar `planes.precio_ars` y poner
> `disponible = true` (ver [despliegue de la web](../tecnico/despliegue-web.md)).

---

## 5. Margen y recupero

Con el plan Pro (neto $11.058 por local por mes) y los costos fijos de $15.248 por mes:

| Locales activos | Margen mensual | Meses para recuperar el desarrollo ($2.063.600) |
|---|---|---|
| 2 | $6.868 | ≈ 300 (no conviene) |
| 10 | $95.329 | ≈ 21,6 |
| 20 | $205.905 | ≈ 10 |
| 50 | $537.635 | ≈ 3,8 |

- **Punto de equilibrio de los costos fijos:** 2 locales (1,4 exactos).
- Con **20 locales** el desarrollo se recupera en unos **10 meses**.
- Los planes anuales mejoran el flujo de caja: cobran 10 meses por adelantado.

---

## 6. Resumen

| Concepto | Monto |
|---|---|
| Desarrollo (único) | $2.063.600 |
| Costos fijos | ≈ $15.250 por mes |
| Costos variables | 7,85 % por venta paga |
| Precio propuesto | Pro $12.000 / 30 días · Max $120.000 / 365 días |
| Recupero estimado | ≈ 10 meses con 20 locales |

---

## Fuentes

Consultadas el 8 de octubre de 2026.

- Valor hora: Develop Argentina, salarios IT 2026 (junior ARS 800.000–1.400.000 mensuales): <https://developargentina.com/estadisticas/salarios-it-argentina-2026>
- Servidor: DigitalOcean, Droplets desde US$4 (512 MB) y US$6 (1 GB); backups semanales 20 %: <https://costbench.com/software/cloud-infrastructure/digitalocean/>
- Mercado Pago: página oficial de Checkout (5,99 % + IVA al instante; 4,19 % + IVA a 10 días): <https://www.mercadopago.com.ar/herramientas-para-vender/check-out>; comparativa 2026 (6,49 % + IVA, 7,85 % efectivo): <https://talo.com.ar/blogs/comisiones-pasarelas-de-pago>
- Fudo, precios en Argentina: <https://fu.do/es-ar/precios/>
- Dólar tarjeta ($2.002, 06/10/2026): <https://www.cronista.com/finanzas-mercados/dolar-hoy-a-cuanto-cotiza-el-oficial-en-los-bancos-de-la-city-este-martes-6-de-octubre/>
- Dominios `.com.ar` (último arancel público encontrado: $475 por año, enero de 2022; por eso se usa un supuesto): <https://www.conclusion.com.ar/internet/actualizan-el-precio-del-registro-de-dominios-de-internet/01/2022/>
