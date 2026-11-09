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
- El articulo debe existir y tener **existencias suficientes en buen estado** (se pregunta al servicio de Inventario).
- **UUID:** si se repite el mismo UUID con los mismos datos se regresa la solicitud ya creada (no se duplica ni se reserva dos veces). Si se repite con datos distintos se responde 409.
- **Cancelacion:** solo se cancela una solicitud PENDIENTE. Se devuelve al inventario unicamente la cantidad de esa solicitud. Cancelar dos veces no devuelve dos veces. Si el inventario no responde, la solicitud sigue PENDIENTE.
- Si otro servicio no responde en 3 segundos (configurable) se regresa 503 y no se guarda nada.

## Conexion con los otros servicios

### Inventario (Servicio 1), `http://127.0.0.1:9000`
Ya existe y **no se modifico**. Solicitudes usa solo estas dos rutas que ya tiene:
- `GET /api/articulos/{id}` para leer el articulo (`cantidadBuenEstado`, `version`, etc.). 404 si no existe.
- `PUT /api/articulos/{id}` para guardar la nueva cantidad. Hay que mandar todos los campos del articulo, incluida su `version`. Si la version no coincide responde 409.

Como Inventario no tiene rutas de reservar ni liberar, Solicitudes hace esto en `ClienteInventario`:
- **Reservar:** lee el articulo y lo guarda con `cantidadBuenEstado` menos la cantidad pedida.
- **Liberar (al cancelar):** lo guarda con `cantidadBuenEstado` mas la cantidad de la solicitud.
- Si Inventario responde 409 (otro proceso cambio el articulo al mismo tiempo), se lee de nuevo y se intenta hasta 3 veces.
- Para arrancar Inventario hay que definir la variable `INVENTARIO_DB_PASSWORD` (viene en su `application.properties`).

### Usuarios (Servicio 3), `http://localhost:8083`
Ya existe y **no se modifico**. Solicitudes usa solo esta ruta que ya tiene:
- `GET /api/usuarios/{id}` responde, entre otros campos, `id`, `nombre`, `habilitado` y `limiteSolicitudes`. 404 si no existe. Los demas campos (`matricula`, `tipo`, fechas...) se ignoran.

Con eso Solicitudes decide: si `habilitado` es false responde 422 (por ejemplo, un usuario suspendido por una incidencia no puede pedir material), y si ya tiene `limiteSolicitudes` solicitudes activas tambien responde 422. Si Usuarios esta apagado o tarda mas de 3 segundos responde 503.

Las direcciones se cambian en `application.properties` (`servicios.usuarios.url` y `servicios.inventario.url`).

## Pruebas
Usan una base H2 en memoria (no necesitan PostgreSQL):
- `SolicitudControladorTests`: prueban las rutas de este servicio y simulan a Usuarios e Inventario.
- `ClienteUsuariosTests`: levantan un Usuarios de mentira con la misma respuesta JSON que el real y prueban usuario habilitado, suspendido, 404, error 500, tiempo de espera agotado y servicio apagado.
- `ClienteInventarioTests`: levantan un Inventario de mentira con el mismo contrato que el real (GET, PUT con `version`, 409, 404) y prueban reservar, liberar, reintento por conflicto, sin existencias y servicio apagado.
En NetBeans: clic derecho en el proyecto > **Test**. Con Maven: `mvn test`.

Cubren: solicitud correcta, confirmar, consultar, cancelar, UUID repetido con los mismos datos y con otros datos, datos invalidos, usuario deshabilitado, limite, sin existencias, cancelar una confirmada, y servicios de Usuarios e Inventario apagados.

## Limitaciones
- Inventario no guarda el UUID de la operacion. El UUID evita duplicados **dentro de Solicitudes**, pero si Inventario guarda el cambio y su respuesta se pierde (o la base de Solicitudes falla justo despues de reservar), un reintento con el mismo UUID puede restar otra vez. Para evitarlo por completo, Inventario tendria que ofrecer una ruta de reservar que reciba el UUID.
- Reservar y liberar cambian `cantidadBuenEstado`: Inventario no distingue entre piezas disponibles y piezas reservadas.
- No incluye SOAP ni la pagina consumidora: eso no es parte del Servicio 2.
