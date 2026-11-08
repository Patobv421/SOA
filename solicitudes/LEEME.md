# Servicio 2: Solicitudes (Equipo 8 - Reservacion de material deportivo)

Este servicio se encarga de las **solicitudes de material deportivo**: crear la solicitud (reserva), confirmarla (asignacion) y cancelarla (libera la reserva).

Es un proyecto Spring Boot con Maven, PostgreSQL y Java 21.

## Que necesitas
- JDK 21 (comprobar con `java -version` y `javac -version`)
- Apache NetBeans (con soporte Maven)
- PostgreSQL
- Postman, opcional, para probar las rutas

## Estructura del proyecto
Sigue la misma organizacion de la practica (modelo, servicio, controlador) y se agregaron `repositorio` para la base de datos:

| Paquete | Que contiene |
|---|---|
| `SolicitudesApplication` | Clase principal que inicia el programa |
| `modelo` | `Solicitud` (tabla), `SolicitudPeticion` (datos de entrada), `UsuarioRespuesta`, `ArticuloRespuesta`, `ErrorRespuesta` |
| `repositorio` | `SolicitudRepositorio` (consultas a PostgreSQL) |
| `servicio` | `SolicitudServicio` (reglas de negocio), `ClienteUsuarios` y `ClienteInventario` (llaman a los otros servicios), `ErrorNegocioException` |
| `controlador` | `SolicitudControlador` (rutas HTTP) y `ManejadorErrores` (respuestas de error) |

## Como abrirlo y ejecutarlo
1. Crear la base de datos: abrir `base_de_datos.sql` en pgAdmin (o psql) y ejecutarlo con el usuario `postgres`.
2. En NetBeans: **File > Open Project** y elegir la carpeta `solicitudes` (la que contiene `pom.xml`). No se crea un proyecto nuevo.
3. Esperar a que Maven descargue las dependencias (necesita internet la primera vez).
4. Clic derecho en el proyecto > **Run**. Tambien se puede ejecutar desde una terminal, en la carpeta que contiene `pom.xml`:
   - Windows: `.\mvnw.cmd spring-boot:run`
   - Linux o macOS: `./mvnw spring-boot:run`
5. Esperar a que la terminal indique que la aplicacion inicio en el puerto 8082. Para detenerla se usa Ctrl + C.

Si PostgreSQL usa otro usuario o contrasena, cambiarlos en `src/main/resources/application.properties`.

## Como comprobar que funciona
Para esto se necesita que los servicios de Usuarios e Inventario esten encendidos (o simularlos). Con Postman o el navegador:

| Solicitud | Estado esperado |
|---|---|
| `GET /api/solicitudes/{uuid}` de una solicitud creada | 200 |
| `GET /api/solicitudes/{uuid}` que no existe | 404 |
| `GET /api/solicitudes/abc` | 400 |
| `POST /api/solicitudes` con cantidad 0 | 400 |
| `POST /api/solicitudes` correcto | 201 |
| Repetir el mismo POST con el mismo UUID | 200 (sin duplicar) |
| Repetir el UUID con otra cantidad | 409 |
| Con el servicio de Inventario apagado | 503 |

## Contrato REST (JSON)

| Metodo | Ruta | Que hace | Respuestas |
|---|---|---|---|
| POST | `/api/solicitudes` | Crea una solicitud (reserva) | 201 creada, 200 reintento con el mismo UUID, 400 datos invalidos, 404 usuario/articulo no existe, 409 UUID usado con otros datos, 422 regla de negocio, 503 otro servicio no responde |
| GET | `/api/solicitudes/{id}` | Consulta una solicitud | 200, 404 |
| POST | `/api/solicitudes/{id}/confirmar` | Asigna el material (PENDIENTE a CONFIRMADA) | 200, 404, 409 si ya estaba cancelada |
| POST | `/api/solicitudes/{id}/cancelar` | Cancela y libera la reserva (PENDIENTE a CANCELADA) | 200, 404, 409 si ya estaba confirmada, 503 |

Ejemplo de cuerpo para crear (el `idOperacion` es un UUID que genera quien llama):

```json
{
	"idOperacion": "3f2b8c1e-5d4a-4c1b-9a77-0e1d2c3b4a5f",
	"idUsuario": 1,
	"idArticulo": 10,
	"cantidad": 2,
	"fechaUso": "2026-12-01"
}
```

Las respuestas de error tienen este formato:

```json
{ "fecha": "2026-11-07T15:00:00", "codigo": 422, "error": "Unprocessable Entity", "mensaje": "No hay existencias suficientes del articulo" }
```

## Reglas de negocio
- Los datos son obligatorios, la cantidad minima es 1 y la fecha de uso no puede ser pasada.
- El usuario debe existir y estar **habilitado** (se pregunta al servicio de Usuarios).
- El usuario no puede pasar su **limite de solicitudes activas** (PENDIENTE o CONFIRMADA).
- El articulo debe existir y tener **existencias suficientes** (se pregunta al servicio de Inventario).
- **UUID:** si se repite el mismo UUID con los mismos datos se regresa la solicitud ya creada (no se duplica ni se reserva dos veces). Si se repite con datos distintos se responde 409.
- **Cancelacion:** solo se cancela una solicitud PENDIENTE. Se libera unicamente la reserva de esa solicitud. Cancelar dos veces no libera dos veces. Si el inventario no responde, la solicitud sigue PENDIENTE.
- Si otro servicio no responde en 3 segundos (configurable) se regresa 503 y no se guarda nada.

## Lo que este servicio espera de los otros servicios
Estas rutas **no vienen en el documento**; las supuse para poder integrar. Hay que acordarlas con los companeros del equipo (si cambian, solo se modifican `ClienteUsuarios` y `ClienteInventario`).

- Usuarios (Servicio 3), puerto 8083: `GET /api/usuarios/{id}` responde `{ "id", "nombre", "habilitado", "limiteSolicitudes" }`. 404 si no existe.
- Inventario (Servicio 1), puerto 8081:
  - `GET /api/articulos/{id}` responde `{ "id", "nombre", "cantidadDisponible" }`. 404 si no existe.
  - `POST /api/articulos/{id}/reservar` con `{ "idSolicitud", "cantidad" }`. Debe repetirse sin duplicar si llega el mismo `idSolicitud`.
  - `POST /api/articulos/{id}/liberar` con `{ "idSolicitud", "cantidad" }`. Libera solo lo reservado para esa solicitud.

Las direcciones se cambian en `application.properties` (`servicios.usuarios.url` y `servicios.inventario.url`).

## Pruebas
Las pruebas estan en `src/test/java/.../SolicitudControladorTests.java` y usan una base H2 en memoria (no necesitan PostgreSQL) y simulan los otros servicios.
En NetBeans: clic derecho en el proyecto > **Test**. Con Maven: `mvn test`.

Cubren: solicitud correcta, confirmar, consultar, cancelar, UUID repetido con los mismos datos y con otros datos, datos invalidos, usuario deshabilitado, limite, sin existencias, cancelar una confirmada, y servicios de Usuarios e Inventario apagados.

## Limitaciones
- Si la base de datos falla justo despues de reservar en el inventario, el cliente debe reintentar con el mismo UUID (el inventario no duplica la reserva).
- No incluye SOAP ni la pagina consumidora: eso no es parte del Servicio 2.
