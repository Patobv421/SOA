# Servicio 3: Usuarios (Equipo 8 - Reservacion de material deportivo)

Este servicio administra a los **solicitantes** (alumnos y docentes): sus datos, si estan **habilitados** para pedir material y cuantas **solicitudes activas** pueden tener. Tambien recibe las **incidencias** que reporta Devoluciones y, segun su gravedad, suspende al usuario.

Es un proyecto Spring Boot con Maven, PostgreSQL y Java 21.

## Que necesitas
- JDK 21 (comprobar con `java -version` y `javac -version`)
- Apache NetBeans (con soporte Maven)
- PostgreSQL
- Postman, opcional, para probar las rutas

## Estructura del proyecto

| Paquete | Que contiene |
|---|---|
| `UsuariosApplication` | Clase principal que inicia el programa |
| `modelo` | `Usuario` e `Incidencia` (tablas), `UsuarioPeticion`, `IncidenciaPeticion`, `EstadoPeticion` (datos de entrada), `UsuarioRespuesta`, `IncidenciaRespuesta`, `ErrorRespuesta` (salida) y los enums `TipoUsuario`, `EstadoUsuario`, `Gravedad` |
| `repositorio` | `UsuarioRepositorio` e `IncidenciaRepositorio` (consultas a PostgreSQL) |
| `servicio` | `UsuarioServicio` e `IncidenciaServicio` (reglas de negocio), `ReglasNegocio` (limites configurables), `ErrorNegocioException` |
| `controlador` | `UsuarioControlador`, `IncidenciaControlador` (rutas HTTP) y `ManejadorErrores` (respuestas de error) |

## Como abrirlo y ejecutarlo
1. Crear la base de datos: abrir `base_de_datos.sql` en pgAdmin (o psql) y ejecutarlo con el usuario `postgres`.
2. En NetBeans: **File > Open Project** y elegir la carpeta `usuarios` (la que contiene `pom.xml`).
3. Esperar a que Maven descargue las dependencias (necesita internet la primera vez).
4. Clic derecho en el proyecto > **Run**. Tambien desde una terminal, en la carpeta que contiene `pom.xml`:
   - Windows: `.\mvnw.cmd spring-boot:run`
   - Linux o macOS: `./mvnw spring-boot:run`
5. Esperar a que la terminal indique que la aplicacion inicio en el puerto **8083**. Las tablas se crean solas.

Si PostgreSQL usa otro usuario o contrasena, cambiarlos en `src/main/resources/application.properties`.

## Datos que administra (base `usuarios_db`)

| Tabla | Que guarda |
|---|---|
| `usuario` | matricula, nombre, correo, tipo (ALUMNO/DOCENTE), estado (ACTIVO/SUSPENDIDO/BAJA), vigencia de la credencial, fecha de fin de suspension |
| `incidencia` | UUID de la operacion, usuario, gravedad (LEVE/GRAVE), descripcion, fecha |

Ningun otro servicio entra a estas tablas: todo pasa por las rutas REST.

## Reglas de negocio
- Matricula (5 a 20 letras o numeros) y correo son obligatorios y **no se pueden repetir**.
- No se registra a nadie con la **credencial vencida**.
- Un usuario esta **habilitado** solo si esta ACTIVO y su credencial sigue vigente. Si no, la respuesta trae el `motivo`.
- **Limite de solicitudes activas** segun el tipo: alumno 2, docente 4. Solicitudes lo usa para no dejar pasar mas.
- Una incidencia **GRAVE** suspende 15 dias.
- **3 incidencias LEVES** en 30 dias suspenden 7 dias.
- Si ya tenia una suspension mas larga, se respeta la mas larga.
- Cuando pasa la fecha de suspension, el usuario vuelve solo a ACTIVO.
- La suspension **no se pone ni se quita a mano**: solo por incidencias. A mano solo se puede dar de BAJA o reactivar a alguien dado de baja.
- **UUID en incidencias:** si Devoluciones repite el mismo UUID con los mismos datos se regresa la misma incidencia (no se cuenta dos veces). Si lo repite con datos distintos, 409.

Los numeros de las reglas estan en `application.properties` (`reglas.*`).

## Contrato REST (JSON)

| Metodo | Ruta | Que hace | Respuestas |
|---|---|---|---|
| POST | `/api/usuarios` | Registra un usuario | 201, 400 datos invalidos, 409 matricula o correo repetido, 422 credencial vencida |
| GET | `/api/usuarios` | Lista los usuarios | 200 |
| GET | `/api/usuarios/{id}` | Consulta un usuario (**la usa Solicitudes**) | 200, 400 id invalido, 404 |
| GET | `/api/usuarios/matricula/{matricula}` | Busca por matricula | 200, 404 |
| PUT | `/api/usuarios/{id}` | Actualiza datos o renueva la credencial | 200, 400, 404, 409 |
| PATCH | `/api/usuarios/{id}/estado` | Dar de BAJA o reactivar (ACTIVO) | 200, 400, 404, 422 |
| POST | `/api/usuarios/{id}/incidencias` | Registra una incidencia (**la usa Devoluciones**) | 201 nueva, 200 reintento, 400, 404, 409 UUID con otros datos |
| GET | `/api/usuarios/{id}/incidencias` | Lista las incidencias del usuario | 200, 404 |

Registrar usuario:
```json
{
	"matricula": "A0012345",
	"nombre": "Ana Lopez",
	"correo": "ana@uni.mx",
	"tipo": "ALUMNO",
	"vigenciaCredencial": "2027-06-30"
}
```

Respuesta de `GET /api/usuarios/1` (Solicitudes lee `habilitado` y `limiteSolicitudes`):
```json
{
	"id": 1, "matricula": "A0012345", "nombre": "Ana Lopez", "correo": "ana@uni.mx",
	"tipo": "ALUMNO", "estado": "ACTIVO", "vigenciaCredencial": "2027-06-30", "suspendidoHasta": null,
	"habilitado": true, "motivo": "Puede solicitar material", "limiteSolicitudes": 2
}
```

Registrar incidencia (lo manda Devoluciones):
```json
{
	"idOperacion": "3f2b8c1e-5d4a-4c1b-9a77-0e1d2c3b4a5f",
	"gravedad": "GRAVE",
	"descripcion": "Balon ponchado"
}
```

Cambiar estado:
```json
{ "estado": "BAJA" }
```

Errores, con el mismo formato que Solicitudes:
```json
{ "fecha": "2026-11-09T15:00:00", "codigo": 422, "error": "Unprocessable Entity", "mensaje": "La credencial ya esta vencida" }
```

## Conexion con los otros servicios
- **Solicitudes (Servicio 2)** llama a `GET /api/usuarios/{id}` antes de crear una solicitud. Si `habilitado` es false responde 422; si ya tiene `limiteSolicitudes` activas tambien responde 422; si Usuarios no existe responde 404; si Usuarios esta apagado responde 503. **Ya funciona con el codigo actual de Solicitudes, sin cambios.**
- **Devoluciones (Servicio 4)** debe llamar a `POST /api/usuarios/{id}/incidencias` cuando registre un dano o un retraso. Asi su servicio cambia de verdad el comportamiento del sistema: un usuario suspendido ya no puede pedir material.

## Prueba integrada (ya verificada con los tres servicios y PostgreSQL)
1. Registrar un alumno en Usuarios: 201, `habilitado: true`.
2. Crear una solicitud en Solicitudes con ese usuario: 201.
3. Registrar una incidencia GRAVE en Usuarios: el usuario queda `SUSPENDIDO`.
4. Crear otra solicitud con ese usuario: Solicitudes responde **422** "El usuario no esta habilitado".
5. Usuario inexistente: Solicitudes responde 404.
6. Apagar Usuarios: Solicitudes responde **503**.
7. Encender Usuarios otra vez: el usuario sigue SUSPENDIDO (los datos se conservan en PostgreSQL).

## Pruebas automaticas
Usan una base H2 en memoria (no necesitan PostgreSQL). En NetBeans: clic derecho > **Test**. Con Maven: `mvn test`.

Cubren: registro correcto, campos que usa Solicitudes, busqueda por matricula, datos invalidos, JSON mal escrito, usuario inexistente, id con letras, matricula repetida, credencial vencida, credencial que vence despues, baja, suspension manual prohibida, incidencia grave, tres leves, incidencia repetida con el mismo UUID y UUID con otros datos.
