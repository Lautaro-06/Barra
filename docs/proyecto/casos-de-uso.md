# Casos de uso — Barra

Especificación de los casos de uso del [documento del proyecto](documento-del-proyecto.md#12-casos-de-uso)
(§12). Cada caso indica actor, condiciones, flujo principal, flujos alternativos y los requisitos
que cubre. Los mensajes entre comillas son los que muestra el sistema.

**Convenciones:** «el sistema» es la app de escritorio (GUI + backend local) o la web de venta,
según el caso. FA = flujo alternativo.

---

## CU-01 Registrar pedido de mostrador

| | |
|---|---|
| Actor principal | Cajero |
| Objetivo | Registrar un pedido para llevar o de mostrador y mandarlo a la cocina |
| Precondiciones | El backend está corriendo («Backend conectado»). Hay productos cargados |
| Disparador | Un cliente pide en el mostrador |
| Pantalla | Vender |
| Requisitos | RF01, RF02, RF05 |
| Estado | Implementado |

**Flujo principal**

1. El cajero abre **Vender**. El sistema muestra los productos con precio y stock.
2. El cajero toca un producto. El sistema lo suma al «Pedido actual» con cantidad 1.
3. El cajero repite el paso 2 o ajusta cantidades con **–**, **+** o quita productos con **x**.
4. (Opcional) El cajero escribe una nota.
5. El sistema recalcula el total en cada cambio.
6. El cajero toca **Confirmar pedido**.
7. El sistema valida disponibilidad y stock de cada producto, registra el pedido en estado
   «en preparación» y descuenta el stock, todo como una sola operación.
8. El sistema muestra «Pedido #N enviado a cocina», vacía el carrito y el pedido aparece en Cocina.

**Flujos alternativos**

- **FA-1 Producto sin stock o no disponible (paso 2):** la tarjeta se ve gris («Sin stock» o «No disponible») y no responde al toque.
- **FA-2 Cantidad mayor al stock visible (paso 3):** el **+** no supera el stock mostrado.
- **FA-3 El stock cambió mientras se armaba el pedido (paso 7):** otro pedido consumió el stock.
  El sistema rechaza el pedido completo con «Stock insuficiente para '…' (pedido: X, stock: Y)»
  y no descuenta nada. El cajero ajusta y vuelve a confirmar.
- **FA-4 Producto pausado mientras tanto (paso 7):** el sistema rechaza con «'…' no está disponible».
- **FA-5 Backend caído (paso 7):** se muestra un aviso rojo y el pedido no se registra; el carrito se conserva.

**Postcondiciones:** el pedido queda guardado con fecha, total y detalle; el stock quedó descontado.

---

## CU-02 Atender mesa (abrir cuenta y sumar rondas)

| | |
|---|---|
| Actor principal | Mozo (también puede hacerlo el cajero) |
| Objetivo | Registrar lo que consume una mesa, en una o más rondas |
| Precondiciones | Backend conectado. La mesa existe |
| Disparador | Se sientan comensales o piden algo más |
| Pantalla | Mesas → ventana de la mesa |
| Requisitos | RF02, RF05, RF09 |
| Estado | Implementado |

**Flujo principal**

1. El mozo abre **Mesas**. El sistema muestra cada mesa como «Libre» (verde) u «Ocupada» (naranja, con el total acumulado).
2. El mozo toca una mesa libre. El sistema abre una cuenta nueva y la mesa pasa a «Ocupada».
3. El sistema muestra la ventana de la mesa: productos a la izquierda; a la derecha las rondas ya pedidas y la ronda actual.
4. El mozo carga los productos de la ronda (igual que en CU-01, pasos 2 a 5).
5. El mozo toca **Agregar a la cuenta**.
6. El sistema valida y registra la ronda como un pedido asociado a la cuenta, descuenta el stock y la manda a la cocina como «Mesa N».
7. El sistema muestra «Ronda agregada a la cuenta» y actualiza el total de la mesa.

**Flujos alternativos**

- **FA-1 Mesa ya ocupada (paso 2):** el sistema retoma la cuenta abierta, sin crear otra.
- **FA-2 Stock insuficiente o producto no disponible (paso 6):** como CU-01 FA-3 y FA-4.
- **FA-3 El mozo cierra la ventana sin agregar nada:** la cuenta sigue abierta. Si no tiene
  rondas, la mesa queda «Ocupada» con $ 0,00 hasta que se cierre (CU-03).

**Postcondiciones:** la cuenta de la mesa acumula la ronda; la cocina la ve en «En preparación».

---

## CU-03 Cerrar cuenta e imprimir ticket

| | |
|---|---|
| Actor principal | Mozo |
| Objetivo | Cobrar la mesa con el detalle de todo lo consumido |
| Precondiciones | La mesa tiene una cuenta abierta |
| Disparador | La mesa pide la cuenta |
| Pantalla | Ventana de la mesa → Ticket |
| Requisitos | RF09 |
| Estado | Implementado |

**Flujo principal**

1. El mozo abre la mesa ocupada.
2. El mozo toca **Cerrar cuenta y generar ticket**.
3. El sistema cierra la cuenta con fecha y hora, deja la mesa «Libre» y devuelve todas las rondas con el total.
4. El sistema muestra el ticket: nombre del local, mesa, apertura, cierre, cada producto con su subtotal, notas y total.
5. El mozo toca **Imprimir** y elige la impresora en el diálogo del sistema operativo.
6. El sistema muestra «Enviado a la impresora».

**Flujos alternativos**

- **FA-1 Ronda actual con productos (paso 2):** el sistema no cierra y avisa «Confirmá o vaciá el carrito antes de cerrar la cuenta».
- **FA-2 Cuenta sin rondas (paso 3):** la mesa se libera y no se muestra ticket.
- **FA-3 No se imprime (paso 5):** el mozo toca **Cerrar**; el ticket se puede leer en pantalla.
- **FA-4 Error de impresión (paso 5):** el sistema muestra «No se pudo imprimir: …».

**Postcondiciones:** la cuenta queda cerrada y cuenta en el resumen diario («cuentas cerradas»).

---

## CU-04 Actualizar estado del pedido

| | |
|---|---|
| Actor principal | Cocina |
| Objetivo | Indicar qué pedidos están listos y cuáles se entregaron |
| Precondiciones | Hay pedidos en preparación o listos |
| Disparador | Se termina de preparar o se entrega un pedido |
| Pantalla | Cocina |
| Requisitos | RF03, RF04 |
| Estado | Implementado |

**Flujo principal**

1. La cocina ve el tablero: «En preparación», «Listo para entregar» y «Entregados (últimos)».
2. Al terminar un pedido, toca **Marcar listo** en su tarjeta.
3. El sistema cambia el estado a «listo» y la tarjeta pasa a la segunda columna.
4. Al entregarlo, toca **Entregar**.
5. El sistema cambia el estado a «entregado» y la tarjeta pasa a «Entregados». La columna muestra los 10 últimos.

**Flujos alternativos**

- **FA-1 Estado inválido (por API):** el sistema responde «Estado inválido».
- **FA-2 Error de conexión:** aviso rojo «No se pudo actualizar el pedido: …»; la tarjeta no se mueve.

**Postcondiciones:** el nuevo estado se ve en todas las pantallas en menos de 4 segundos.
Entregar un pedido de mesa no cierra la cuenta (eso es CU-03).

---

## CU-05 Gestionar productos y stock

| | |
|---|---|
| Actor principal | Administrador / dueño |
| Objetivo | Mantener el catálogo, los precios, el stock y los umbrales |
| Precondiciones | Backend conectado |
| Pantalla | Admin → Productos → formulario de producto |
| Requisitos | RF05, RF08 |
| Estado | Implementado (sin borrado físico) |

**Flujo principal (alta)**

1. El administrador toca **+ Nuevo producto**.
2. Completa nombre, precio, stock, umbral (opcional) y «Disponible para vender».
3. Toca **Guardar**. El sistema valida (nombre no vacío, precio > 0, stock y umbral ≥ 0), crea el producto y muestra «Producto "…" creado».

**Flujo principal (modificación y reposición)**

1. El administrador hace doble clic en la fila del producto.
2. Cambia los datos. Para reponer, escribe el **stock total** nuevo.
3. Toca **Guardar**. El sistema actualiza solo lo enviado y muestra «Producto "…" actualizado».

**Flujos alternativos**

- **FA-1 Datos inválidos:** «El nombre no puede estar vacío y el precio debe ser mayor a 0.» o «Precio, stock o umbral inválido.»
- **FA-2 Pausar (baja lógica):** se destilda «Disponible para vender»; el producto deja de poder venderse sin perder su stock.
- **FA-3 Umbral vacío:** el producto usa el umbral global de Configuración.

**Postcondiciones:** el catálogo actualizado se ve en Vender y en las mesas en menos de 4 segundos.
Si el stock repuesto supera el umbral, la alerta de ese producto se rearma (CU-08).

---

## CU-06 Gestionar mesas del salón

| | |
|---|---|
| Actor principal | Administrador |
| Objetivo | Adaptar la cantidad y nombres de mesas al local |
| Pantalla | Admin → Mesas |
| Requisitos | RF09 |
| Estado | Implementado |

**Flujo principal**

1. El administrador escribe el nombre («Mesa 7», «Vereda 1») y toca **+ Agregar mesa**.
2. El sistema crea la mesa libre y muestra «Mesa "…" agregada».
3. Para quitar una mesa libre, toca **Eliminar**; el sistema la saca del salón.

**Flujos alternativos**

- **FA-1 Nombre vacío:** «Ponele un nombre a la mesa».
- **FA-2 Mesa ocupada:** el botón Eliminar está deshabilitado; por API se responde «No se puede borrar una mesa con la cuenta abierta».
- **FA-3 Mesa que ya tuvo cuentas:** se elimina igual (baja lógica). Sus cuentas cerradas siguen en el historial de ventas y en el resumen diario. Ver CP-17 y DEF-01 del [plan de pruebas](plan-de-pruebas.md).

---

## CU-07 Configurar local, dueño y email

| | |
|---|---|
| Actor principal | Administrador / dueño |
| Objetivo | Personalizar el local y habilitar alertas y resumen por email |
| Pantalla | Admin → Configuración |
| Requisitos | RF06, RF10, RF11, RF12 |
| Estado | Implementado |

**Flujo principal**

1. El administrador carga el **nombre del local** y toca **Guardar**: el nombre aparece en la barra lateral, el título y el ticket.
2. Carga **nombre, email y teléfono del dueño** y toca **Guardar datos del dueño**.
3. Carga umbral global, casillas de alertas y resumen, hora del resumen, email de destino (opcional) y datos SMTP. Toca **Guardar configuración de email**.
4. El sistema valida el estado final: si alguna casilla está tildada, exige servidor, usuario, contraseña y un destino (propio o el del dueño). Cifra la contraseña y guarda.
5. Toca **Enviar email de prueba**. El sistema manda el mail con la configuración guardada y muestra «Email de prueba enviado a …».

**Flujos alternativos**

- **FA-1 Datos incompletos (paso 4):** «Para habilitar el email hacen falta: …».
- **FA-2 Formato inválido:** email del dueño o de destino inválido, hora fuera de HH:MM, puerto o umbral no numérico.
- **FA-3 Credenciales rechazadas (paso 5):** «El servidor SMTP rechazó el usuario/contraseña (en Gmail hace falta una 'contraseña de aplicación')».
- **FA-4 Servidor sin STARTTLS en el puerto 587:** no se envía la contraseña sin cifrar; se sugiere el puerto 465.
- **FA-5 Sin conexión (paso 5):** «No se pudo mandar el email: …».
- **FA-6 Contraseña ya guardada:** si el campo queda vacío, se conserva la anterior.

---

## CU-08 Alertar stock bajo

| | |
|---|---|
| Actor principal | Temporizador del sistema (hilo `stock-watcher`) |
| Beneficiario | Dueño del local |
| Actor secundario | Servicio de email |
| Precondiciones | «Enviar alertas de stock bajo por email» tildado y SMTP configurado |
| Disparador | Cada 30 segundos |
| Requisitos | RF06 |
| Estado | Implementado |

**Flujo principal**

1. El sistema calcula el umbral efectivo de cada producto (propio o global).
2. Actualiza la lista de productos con stock bajo (`GET /alertas`).
3. Junta los productos bajo el umbral que todavía no fueron avisados.
4. Manda **un** mail con todos ellos: «[Local] Stock bajo: …» o «[Local] Stock bajo en N productos».
5. Marca esos productos como avisados.

**Flujos alternativos**

- **FA-1 Producto repuesto:** si su stock vuelve a estar en o por encima del umbral, se desmarca y se volverá a avisar si baja otra vez.
- **FA-2 Falla el envío:** se registra el error y se reintenta a los 5 minutos; los productos siguen pendientes.
- **FA-3 Email deshabilitado:** no se marca nada; al habilitarlo llega un mail con todo lo que esté bajo en ese momento.

---

## CU-09 Enviar resumen de ventas

| | |
|---|---|
| Actor principal | Temporizador (hilo `resumen-diario`) o administrador («Enviar resumen ahora») |
| Beneficiario | Dueño |
| Precondiciones | SMTP configurado. Para el envío automático: «Enviar resumen diario» tildado |
| Requisitos | RF07, RF12 |
| Estado | Implementado |

**Flujo principal (automático)**

1. Cada 30 segundos el sistema revisa si pasó la hora configurada y si ese día ya se mandó.
2. Si corresponde, calcula el resumen desde el último envío (o las últimas 24 h): total, pedidos,
   ticket promedio, mostrador y mesas, cuentas cerradas, 10 productos más vendidos y stock bajo.
3. Lo manda por email y registra la fecha del envío.

**Flujo alternativo — «Enviar resumen ahora»:** el administrador toca el botón; el sistema manda
el resumen de las últimas 24 h y muestra «Resumen enviado a …». No reemplaza al automático.

**Otros flujos alternativos**

- **FA-1 Falla el envío:** reintento a los 5 minutos.
- **FA-2 Backend apagado a la hora del resumen:** se manda al volver a abrir ese día; si pasa a otro
  día, esas ventas se suman al siguiente resumen (hasta dos días hacia atrás).

---

## CU-10 Respaldar la base de datos

| | |
|---|---|
| Actor principal | Temporizador (hilo `db-backup`) |
| Disparador | Al arrancar el backend y cada 4 horas |
| Requisitos | RF13, RNF08 |
| Estado | Implementado |

**Flujo principal**

1. El sistema copia `barra.db` a `backups/barra_backup_AAAAMMDD_HHMMSS.db` con la API de backup de SQLite.
2. Registra «OK | archivo | tamaño» en `backups/backups.log`.
3. Si hay más de 5 copias, borra las más viejas.

**Flujo alternativo — FA-1 Falla la copia:** registra «ERROR | motivo» en el log y lo vuelve a intentar en el siguiente intervalo.

---

## CU-11 Obtener licencia en la web

| | |
|---|---|
| Actor principal | Comprador |
| Actores secundarios | Mercado Pago, servicio de email |
| Pantalla | Web: inicio → checkout → resultado |
| Requisitos | RF14, RF19 |
| Estado | Implementado (plan Gratis habilitado; planes pagos listos y deshabilitados) |

**Flujo principal (plan Gratis)**

1. El comprador entra a la web; el sistema lista los planes.
2. Toca **Elegir plan** en Gratis, completa nombre y email y toca **Continuar**.
3. El sistema registra al comprador, genera el código (`BARRA-XXXX-XXXX-XXXX`) y la clave secreta,
   guarda solo el hash de la clave, registra un pago «gratuito» y manda el mail «Tu licencia de
   Barra» con código, clave y link de descarga.
4. El sistema muestra «¡Listo!».

**Flujo alternativo — plan pago**

3a. El sistema registra un pago «pendiente», crea la preferencia en Mercado Pago y redirige al comprador.
3b. El comprador paga. Mercado Pago avisa por webhook firmado.
3c. El sistema verifica la firma, consulta el pago y, si está aprobado, emite la licencia y manda el mail. Si se rechazó, marca el pago «rechazado».
3d. Mercado Pago devuelve al comprador a «¡Listo!» o a «El pago no se pudo procesar».

**Otros flujos alternativos**

- **FA-1 Plan no disponible:** «Plan no disponible» (no se crea nada).
- **FA-2 Datos faltantes:** «plan_id, comprador.nombre y comprador.email son requeridos».
- **FA-3 Webhook con firma inválida:** se ignora (y se responde 200 para que Mercado Pago no reintente).

---

## CU-12 Recuperar licencia

| | |
|---|---|
| Actor principal | Comprador |
| Pantalla | Web: Recuperar licencia |
| Requisitos | RF17 |
| Estado | Implementado |

**Flujo principal**

1. El comprador ingresa el email con el que compró y toca **Recuperar licencia**.
2. El sistema busca su licencia más reciente y le manda el **código** por mail (nunca la clave).
3. El sistema muestra «Si el email está registrado, te llega un mail con tu código…».

**Flujo alternativo — FA-1 Email no registrado:** se muestra el mismo mensaje y no se manda nada (no se revela qué emails existen).

---

## CU-13 Administrar ventas y licencias

| | |
|---|---|
| Actor principal | Administrador de la web (equipo de Barra) |
| Pantalla | Web: `/admin` |
| Requisitos | RF18 |
| Estado | Implementado |

**Flujo principal**

1. El administrador entra a `/admin` e ingresa usuario y contraseña.
2. El sistema verifica las credenciales y muestra las ventas (comprador, email, plan, monto, estado) y las licencias (código, comprador, plan, activaciones, vencimiento).
3. Para dar de baja una licencia, toca **Revocar**. El sistema la deja sin activaciones disponibles: ya no se puede activar y su estado pasa a no válido.

**Flujo alternativo — FA-1 Credenciales incorrectas:** «Usuario o contraseña incorrectos».

Guía de uso: [`docs/tecnico/manual-admin-web.md`](../tecnico/manual-admin-web.md).

---

## CU-14 Activar licencia en la app (pendiente)

| | |
|---|---|
| Actor principal | Administrador del local |
| Actor secundario | Servicio de licencias (backend web) |
| Requisitos | RF15, RF16, RF20 |
| Estado | **Pendiente en la app.** Los endpoints `POST /api/licencias/activar` y `GET /api/licencias/estado` ya existen y están probados |

**Flujo previsto**

1. Al abrir la app por primera vez, el sistema pide código y clave.
2. El sistema consulta `POST /api/licencias/activar`; si es válida, guarda la licencia y la fecha de vencimiento.
3. En cada arranque, el sistema revalida con `GET /api/licencias/estado` cuando hay internet; sin internet, usa la fecha guardada.
4. Al vencer, el sistema avisa y bloquea la toma de pedidos hasta renovar.

**Flujos alternativos previstos:** clave incorrecta («Credenciales inválidas»), sin activaciones disponibles («Licencia sin activaciones disponibles»), licencia revocada.

---

## CU-15 Modificar o cancelar pedido (pendiente)

| | |
|---|---|
| Actor principal | Cajero |
| Requisitos | RF21 |
| Estado | **Pendiente** |

**Flujo previsto**

1. El cajero selecciona un pedido en preparación.
2. Lo modifica (cantidades) o lo cancela indicando un motivo.
3. El sistema ajusta el stock (devuelve lo cancelado), recalcula el total y avisa a la cocina.

**Regla prevista:** un pedido «entregado» no se puede cancelar; las cancelaciones se informan en el resumen diario.
