"""
concurrency.py

Punto 1 de la rama app/concurrence: pool de hilos real para procesar
pedidos concurrentes.

FastAPI ya corre cada función sync en un threadpool propio (vía anyio),
pero eso queda oculto y sin control nuestro. Acá creamos y administramos
explícitamente un concurrent.futures.ThreadPoolExecutor: los pedidos que
llegan se delegan a este pool, con una cantidad fija de workers ("mozos"
de la barra). Si llegan más pedidos que mozos disponibles, se encolan
solos -> ThreadPoolExecutor maneja la cola internamente, no hace falta
programarla a mano.

La sección crítica (leer/descontar stock) sigue protegida por write_lock
en database.py, así que aunque varios workers procesen pedidos "al mismo
tiempo", el acceso a barra.db sigue siendo seguro.
"""

from concurrent.futures import ThreadPoolExecutor

# Cantidad de pedidos que se pueden procesar en simultáneo.
MAX_WORKERS = 4

pedido_executor = ThreadPoolExecutor(
    max_workers=MAX_WORKERS,
    thread_name_prefix="pedido-worker",
)


def shutdown_executor(wait: bool = True) -> None:
    """Se llama al apagar la app (evento shutdown de FastAPI) para que los
    hilos del pool no queden colgados."""
    pedido_executor.shutdown(wait=wait)
