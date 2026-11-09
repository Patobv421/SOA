package com.proyecto.solicitudes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.proyecto.solicitudes.modelo.UsuarioRespuesta;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Pruebas de la comunicacion con el servicio de Usuarios
// Aqui se levanta un Usuarios de mentira que responde con el mismo JSON que el real
// (trae campos de mas como matricula, tipo y fechas, que Solicitudes debe ignorar)
class ClienteUsuariosTests {

	private HttpServer servidor;
	private ClienteUsuarios clienteUsuarios;

	@BeforeEach
	void prepararServidor() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/api/usuarios/", this::atender);
		servidor.start();

		String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
		clienteUsuarios = new ClienteUsuarios(url, 1);
	}

	@AfterEach
	void detenerServidor() {
		servidor.stop(0);
	}

	// ---------- Pruebas ----------

	@Test
	void usuario_habilitado_se_lee_aunque_traiga_campos_de_mas() {
		UsuarioRespuesta usuario = clienteUsuarios.obtenerUsuario(1L);

		assertEquals(1L, usuario.getId());
		assertEquals("Ana Lopez", usuario.getNombre());
		assertTrue(usuario.isHabilitado());
		assertEquals(2, usuario.getLimiteSolicitudes());
	}

	@Test
	void usuario_suspendido_no_esta_habilitado() {
		UsuarioRespuesta usuario = clienteUsuarios.obtenerUsuario(2L);

		assertFalse(usuario.isHabilitado());
	}

	@Test
	void usuario_que_no_existe_da_404() {
		ErrorNegocioException error = assertThrows(ErrorNegocioException.class, () -> clienteUsuarios.obtenerUsuario(99L));

		assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
	}

	@Test
	void si_usuarios_falla_con_error_500_se_responde_503() {
		ErrorNegocioException error = assertThrows(ErrorNegocioException.class, () -> clienteUsuarios.obtenerUsuario(3L));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getEstado());
	}

	@Test
	void si_usuarios_tarda_demasiado_se_responde_503() {
		// El usuario 4 responde despues de 3 segundos y el tiempo de espera es 1 segundo
		ErrorNegocioException error = assertThrows(ErrorNegocioException.class, () -> clienteUsuarios.obtenerUsuario(4L));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getEstado());
	}

	@Test
	void usuarios_apagado_da_503() throws IOException {
		// Se busca un puerto libre donde nadie esta escuchando
		int puertoLibre;
		try (ServerSocket socket = new ServerSocket(0)) {
			puertoLibre = socket.getLocalPort();
		}
		ClienteUsuarios clienteSinServidor = new ClienteUsuarios("http://127.0.0.1:" + puertoLibre, 1);

		ErrorNegocioException error = assertThrows(ErrorNegocioException.class, () -> clienteSinServidor.obtenerUsuario(1L));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getEstado());
	}

	// ---------- Usuarios de mentira ----------

	// Atiende GET /api/usuarios/{id} como lo hace el servicio real
	private void atender(HttpExchange intercambio) throws IOException {
		String ruta = intercambio.getRequestURI().getPath();

		if (ruta.equals("/api/usuarios/1")) {
			responder(intercambio, 200, usuarioJson("ACTIVO", "null", true, "Puede solicitar material"));
		} else if (ruta.equals("/api/usuarios/2")) {
			responder(intercambio, 200, usuarioJson("SUSPENDIDO", "\"2026-12-01\"", false, "El usuario esta suspendido hasta 2026-12-01"));
		} else if (ruta.equals("/api/usuarios/3")) {
			responder(intercambio, 500, "{\"codigo\":500,\"error\":\"Internal Server Error\",\"mensaje\":\"Fallo interno\"}");
		} else if (ruta.equals("/api/usuarios/4")) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			responder(intercambio, 200, usuarioJson("ACTIVO", "null", true, "Puede solicitar material"));
		} else {
			responder(intercambio, 404, "{\"codigo\":404,\"error\":\"Not Found\",\"mensaje\":\"El usuario no existe\"}");
		}
	}

	// Arma el JSON del usuario con el mismo formato que regresa el servicio de Usuarios
	private String usuarioJson(String estado, String suspendidoHasta, boolean habilitado, String motivo) {
		return "{\"id\":" + (habilitado ? 1 : 2) + ",\"matricula\":\"A0012345\",\"nombre\":\"Ana Lopez\","
			+ "\"correo\":\"ana@uni.mx\",\"tipo\":\"ALUMNO\",\"estado\":\"" + estado + "\","
			+ "\"vigenciaCredencial\":\"2027-06-30\",\"suspendidoHasta\":" + suspendidoHasta + ","
			+ "\"habilitado\":" + habilitado + ",\"motivo\":\"" + motivo + "\",\"limiteSolicitudes\":2}";
	}

	private void responder(HttpExchange intercambio, int codigo, String cuerpo) throws IOException {
		byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
		intercambio.getResponseHeaders().add("Content-Type", "application/json");
		try {
			intercambio.sendResponseHeaders(codigo, bytes.length);
			intercambio.getResponseBody().write(bytes);
		} finally {
			intercambio.close();
		}
	}

}
