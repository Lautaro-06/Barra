# Pruebas automatizadas de Barra

Campaña de pruebas de caja negra (por HTTP) del [plan de pruebas](../docs/proyecto/plan-de-pruebas.md).
Cada script levanta el sistema desde cero, ejecuta los casos y guarda los resultados en
`pruebas/resultados/`.

| Script | Qué prueba | Casos |
|---|---|---|
| `test_escritorio.py` | Backend de escritorio: productos, pedidos, mesas, configuración, emails (alertas, prueba, resumen), concurrencia, persistencia, backup | CP-01 a CP-34 |
| `test_web.py` | Backend de la web de venta: planes, compra gratis, licencias, recuperación, panel admin, webhook de Mercado Pago | CW-01 a CW-17 |
| `lanzar_backend.py` | Auxiliar de `test_escritorio.py`: levanta el backend confiando en el certificado del SMTP de prueba | — |

## Backend de escritorio

**Es seguro:** trabaja sobre una copia temporal de `application/barra-backend`, nunca sobre tu
`barra.db`. Levanta un servidor SMTP de prueba en `127.0.0.1:8025` (con STARTTLS y un
certificado autofirmado que genera solo) para recibir las alertas y los resúmenes.

```bash
# Desde la raíz del repo, con el entorno virtual del backend activado
pip install -r application/barra-backend/requirements.txt -r pruebas/requirements.txt
python pruebas/test_escritorio.py
```

- Necesita los puertos **8000** y **8025** libres: cerrá Barra antes de correrlo.
- Tarda cerca de 1 minuto (espera a que los hilos de stock y resumen hagan su trabajo; los
  intervalos se acortan a 1 segundo solo durante la prueba).

## Backend de la web de venta

Usa una base propia, **`barra_web_pruebas`**, que borra y vuelve a crear en cada corrida.
Nunca toca `barra_web`.

Requisitos:

1. `npm ci` en `barraPagina/barraWebBackend`.
2. MySQL o MariaDB corriendo en `127.0.0.1:3306` y el cliente `mysql` en el PATH.
3. Un usuario con permiso sobre `barra_web_pruebas`:

   ```sql
   CREATE USER IF NOT EXISTS 'barra'@'localhost' IDENTIFIED BY 'barra-dev';
   GRANT ALL ON barra_web_pruebas.* TO 'barra'@'localhost';
   ```

   Si usás otro usuario, indicalo con `PRUEBAS_DB_USER` y `PRUEBAS_DB_PASSWORD`.
4. El puerto **4000** libre.

```bash
python pruebas/test_web.py
```

Los mails de prueba (con códigos y claves de licencia de prueba) se leen de la consola del
backend, que queda en un archivo temporal (`barra-pruebas-web.log`) y no se versiona.

## Resultados

Cada script imprime una línea por caso (`OK` o `XX`) y deja un JSON en `pruebas/resultados/`
con el ID del caso, el requisito, el resultado esperado, el obtenido y el estado. Los resultados
de la última corrida están documentados en el [plan de pruebas](../docs/proyecto/plan-de-pruebas.md).
