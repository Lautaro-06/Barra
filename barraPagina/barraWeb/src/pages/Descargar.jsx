import Header from "../components/header.jsx";
import Footer from "../components/footer.jsx";
import Card from "../components/card.jsx";
import Button from "../components/button.jsx";

const VERSION = "1.0.0";

// Los archivos se sirven desde public/downloads/. Al publicar una versión
// nueva se reemplazan ahí y se actualiza esta lista (nombre y tamaño).
const ARCHIVOS = [
  {
    nombre: "Servidor de Barra",
    descripcion:
      "Guarda los productos, pedidos y ventas de tu local. Tiene que estar abierto mientras usás Barra.",
    archivo: `barra-backend-v${VERSION}.exe`,
    requisito: "Windows de 64 bits",
    tamaño: "18,9 MB",
  },
  {
    nombre: "Aplicación de Barra",
    descripcion: "Las pantallas de caja, mesas, cocina y administración del local.",
    archivo: `barra-gui-v${VERSION}.jar`,
    requisito: "Java 17 o superior",
    tamaño: "101 KB",
  },
];

function IconoDescarga() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 20 20"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="mr-2 h-4 w-4"
    >
      <path d="M10 3v10M5.5 8.5 10 13l4.5-4.5M4 16.5h12" />
    </svg>
  );
}

function Archivo({ nombre }) {
  return <code className="rounded bg-surface px-1.5 py-0.5 text-[0.8125rem] text-primary">{nombre}</code>;
}

export default function Descargar() {
  const [servidor, aplicacion] = ARCHIVOS;

  return (
    <div className="flex min-h-screen flex-col">
      <Header />

      <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
        <span className="inline-block rounded-full bg-success/10 px-3 py-1 text-xs font-semibold text-success">
          Por ahora la descarga es gratuita
        </span>
        <h1 className="mt-4 text-3xl font-bold text-primary">Descargá Barra</h1>
        <p className="mt-2 text-muted">
          Versión {VERSION}. Son dos archivos: el servidor, que guarda los datos de tu local, y la
          aplicación que usan la caja y la cocina. Descargá los dos.
        </p>

        <div className="mt-10 grid grid-cols-1 gap-6 sm:grid-cols-2">
          {ARCHIVOS.map((item) => (
            <Card key={item.archivo} className="flex flex-col gap-4">
              <div>
                <h2 className="text-lg font-bold text-primary">{item.nombre}</h2>
                <p className="mt-1 text-sm text-muted">
                  {item.requisito} · {item.tamaño}
                </p>
                <p className="mt-3 text-sm text-text">{item.descripcion}</p>
                <p className="mt-3">
                  <Archivo nombre={item.archivo} />
                </p>
              </div>

              <Button as="a" href={`/downloads/${item.archivo}`} download className="mt-auto">
                <IconoDescarga />
                Descargar
              </Button>
            </Card>
          ))}
        </div>

        <Card className="mt-10">
          <h2 className="text-lg font-bold text-primary">Cómo instalarlo</h2>
          <ol className="mt-4 flex list-decimal flex-col gap-3 pl-5 text-sm text-text marker:font-semibold marker:text-primary">
            <li>
              Si no tenés Java 17 o superior, instalalo desde{" "}
              <a
                href="https://adoptium.net/"
                target="_blank"
                rel="noreferrer"
                className="font-semibold text-cta hover:text-cta-hover"
              >
                adoptium.net
              </a>
              .
            </li>
            <li>
              Creá una carpeta para Barra (por ejemplo <Archivo nombre="C:\Barra" />) y guardá ahí los
              dos archivos. En esa carpeta quedan los datos de tu local y las copias de seguridad
              automáticas.
            </li>
            <li>
              Abrí <Archivo nombre={servidor.archivo} />. Se abre una ventana de consola: dejala abierta
              mientras uses Barra. Si Windows muestra "Windows protegió tu PC", tocá "Más información"
              y después "Ejecutar de todas formas".
            </li>
            <li>
              Abrí <Archivo nombre={aplicacion.archivo} /> con doble clic. Abajo del menú lateral tiene
              que decir "Backend conectado". Si dice "Backend caído", revisá que la ventana del paso
              anterior siga abierta.
            </li>
          </ol>
        </Card>
      </main>

      <Footer />
    </div>
  );
}