![Banner Logo](./docs/design/Barra%20-%20Banner.png)

## Descripcion

Sistema interno de gestión de pedidos y stock pensado para comercios gastronómicos locales (restaurantes, bares, panaderías, etc). No es una app de cara al cliente, sino que está diseñada para que el propio negocio controle su operación diaria de forma simple y ordenada.

## Objetivo

Hoy en día muchos comercios gastronómicos chicos y medianos manejan su stock y sus pedidos internos con planillas sueltas, cuadernos o sistemas pensados para operaciones mucho más grandes. Barra busca ofrecer una alternativa liviana, enfocada específicamente en la gestión interna (pedidos y stock), sin la complejidad ni el costo de plataformas más completas como Fudo, HivePOS o Pedix.

## Características principales

- Gestión de stock en tiempo real
- Registro y seguimiento de pedidos internos
- Aplicación de escritorio para uso en el local
- Sitio web para venta y administración de licencias del sistema

*(Esta lista se va a ir completando a medida que se definan más casos de uso)*

## Tecnologías utilizacas

### Aplicación de escritorio
- Frontend: JavaFX / Electron + React
- Backend: Python (Flask / FastAPI)
- Comunicación: HTTP local entre frontend y backend
- Base de datos: SQLite

### Sitio de ventas y licencias
- Frontend: React + Vite
- Backend: Node.js + Express
- Base de datos: MySQL
- Pagos: Mercado Pago

## Arquitectura

La app de escritorio funciona de forma local: el frontend (JavaFX o Electron/React) se comunica con un backend en Python vía HTTP local, y los datos se persisten en SQLite. El sitio web de ventas es independiente, con su propio backend en Node/Express y base de datos MySQL, encargado de gestionar licencias y pagos.

## Instalación

*(Pendiente — se va a completar con los pasos para levantar el backend, el frontend y el sitio de ventas)*

## Estructura del repositorio

```
Barra/
├── application/ # Archivos Aplicación Java + Python
│   ├── barra-backend/ # Python con Flask y FastAPI (Intérprete: Venv)
│   │   ├── app/ # Archivos Python utilizados para crear el backend 
│   │   ├── dist/ # Carpeta que tiene el ejecutable
│   │   │   └── barra-backend.exe # Ejecutable hecho con PyInstaller
│   │   ├── run_backend.py # Llama a los archivos necesarios en app/ para hacer el ejecutable
│   │   ├── README.md
│   │   └── requirements.txt # Librerías instaladas
│   ├── barra-gui/ # JavaFX con Electron y React
│   │   ├── src/main/java/com/barra/gui/ # Archivos de las clases en texto plano
│   │   ├── target/ # Archivos compilados en bytecode
│   │   │   └── barra-gui-1.0.0.jar # Ejecutable GUI Barra
│   │   ├── README.md
│   │   └── pom.xml # Compila, prueba y empaqueta el proyecto
├── barraPagina/ # Full-stack de la página de venta e instalación
│   ├── barraWeb/ # Frontend con React + Vite
│   │   ├──src/
│   │   │   ├──components/ # Componentes reutilizales
│   │   │   ├── pages/ # Vistas/páginas
│   │   │   ├── services/ # Requests HTTP
│   │   │   ├── styles/ # Diseño de la página
│   │   │   ├── app.jsx
│   │   │   └── main.jsx
│   │   ├── README.md
│   │   ├── index.html
│   │   ├── package-lock.json
│   │   ├── package.json
│   │   ├── postcss.config.json
│   │   ├── tailwind.config.json
│   │   └── vite.config.json
│   ├── barraWebBackend/ # Backend con JavaScript y MySQL
│   │   ├── src/
│   │   │   ├── controllers/ # Controladores HTTP
│   │   │   ├── middleware/ # Auth y validaciones
│   │   │   ├── models/ # Modelos Sequelize
│   │   │   ├── routes/ # Rutas HTTP
│   │   │   ├── services/ # Email y forma de pago
│   │   │   └── utils/ # Utilidades compartidas
│   │   ├── app.js
│   │   └── db.js # Configuración base de datos
│   └── docs/ # Documentación
└── README.md # Este documento
```

## Plan de trabajo
 
El desarrollo está planificado en un Gantt de 8 semanas. Estado actual: etapa de planificación y documentación (requisitos, actores, casos de uso, diagrama ER, análisis FODA e investigación de competidores ya definidos).
