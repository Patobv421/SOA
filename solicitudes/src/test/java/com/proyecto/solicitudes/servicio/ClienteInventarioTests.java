package com.proyecto.solicitudes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.proyecto.solicitudes.modelo.ArticuloRespuesta;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Pruebas de la comunicacion con el servicio de Inventario
// Aqui se levanta un Inventario de mentira que responde igual que el real
// (GET y PUT en /api/articulos/10, con el campo version y error 409 si no coincide)
class ClienteInventarioTests {

	private HttpServer servidor;
	private ClienteInventario clienteInventario;

	// Estado del inventario de mentira
	private int cantidadBuenEstado;
	private long version;
	private int conflictosPendientes;
	private int cantidadDePuts;
	private String ultimoCuerpoPut;

	@BeforeEach
	void prepararServidor() throws IOException {
		cantidadBuenEstado = 10;
		version = 3;
		conflictosPendientes = 0;
		cantidadDePuts = 0;
		ultimoCuerpoPut = "";

		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/api/articulos/", this::atender);
		servidor.start();

		String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
		clienteInventario = new ClienteInventario(url, 2);
	}

	@AfterEach
	void detenerServidor() {
		servidor.stop(0);
	}

	// ---------- Pruebas ----------

	@Test
	void obtener_articulo_lee_las_existencias_y_la_version() {
		ArticuloRespuesta articulo = clienteInventario.obtenerArticulo(10L);

		assertEquals("Balon de futbol", articulo.getNombre());
		assertEquals(10, articulo.getCantidadBuenEstado());
		assertEquals(3L, articulo.getVersion());
	}

	@Test
	void reservar_resta_de_buen_estado_y_manda_la_version() {
		clienteInventario.reservar(10L, 4);

		assertEquals(6, cantidadBuenEstado);
		assertEquals(4L, version);
		assertEquals(1, cantidadDePuts);
		assertTrue(ultimoCuerpoPut.contains("\"version\":3"));
		assertTrue(ultimoCuerpoPut.contains("\"cantidadBuenEstado\":6"));
	}

	@Test
	void liberar_devuelve_la_cantidad_a_buen_estado() {
		clienteInventario.liberar(10L, 3);

		assertEquals(13, cantidadBuenEstado);
	}

	@Test
	void si_otro_proceso_cambia_el_articulo_se_vuelve_a_intentar() {
		// El primer PUT recibe un 409 y el segundo ya funciona
		conflictosPendientes = 1;

		clienteInventario.reservar(10L, 2);

		assertEquals(8, cantidadBuenEstado);
		assertEquals(2, cantidadDePuts);
	}

	@Test
	void si_siempre_hay_conflicto_se_rinde_sin_cambiar_nada() {
		conflictosPendientes = 10;

		ErrorNegocioException error = assertThrows(ErrorNegocioException.class, () -> clienteInventario.reservar(10L, 2));

		assertEquals(HttpStatus.CONFLICT, error.getEstado());
		assertEquals(3, cantidadDePuts);
		assertEquals(10, cantidadBuenEstado);
	}

	@Test
	void no_se_puede_reservar_mas_de_las_existencias() {
		ErrorNegocioException error = assertThrows(ErrorNegocioException.class, () -> clienteInventario.reservar(10L, 11));

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getEstado());
		assertEquals(0, cantidadDePuts);
	}

	@Test
	void articulo_que_no_existe_da_404() {
		ErrorNegocioException error = assertThrows(ErrorNegocioException.class, () -> clienteInventario.obtenerArticulo(99L));

		assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
	}

	@Test
	void inventario_apagado_da_503() throws IOException {
		// Se busca un puerto libre donde nadie esta escuchando
		int puertoLibre;
		try (ServerSocket socket = new ServerSocket(0)) {
			puertoLibre = socket.getLocalPort();
		}
		ClienteInventario clienteSinServidor = new ClienteInventario("http://127.0.0.1:" + puertoLibre, 1);

		ErrorNegocioException error = assertThrows(ErrorNegocioException.class, () -> clienteSinServidor.obtenerArticulo(10L));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getEstado());
	}

	// ---------- Inventario de mentira ----------

	// Atiende las peticiones que manda ClienteInventario
	private void atender(HttpExchange intercambio) throws IOException {
		String ruta = intercambio.getRequestURI().getPath();
		String metodo = intercambio.getRequestMethod();

		if (!ruta.equals("/api/articulos/10")) {
			responder(intercambio, 404, "{\"estado\":404,\"mensaje\":\"No existe el articulo\"}");
		} else if (metodo.equals("GET")) {
			responder(intercambio, 200, articuloJson());
		} else if (metodo.equals("PUT")) {
			String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			cantidadDePuts++;
			ultimoCuerpoPut = cuerpo;

			if (conflictosPendientes > 0) {
				// Simula que otro proceso modifico el articulo y cambio su version
				conflictosPendientes--;
				version++;
				responder(intercambio, 409, "{\"estado\":409,\"mensaje\":\"El articulo fue modificado\"}");
			} else if (numeroDe(cuerpo, "version") != version) {
				responder(intercambio, 409, "{\"estado\":409,\"mensaje\":\"La version no coincide\"}");
			} else {
				cantidadBuenEstado = (int) numeroDe(cuerpo, "cantidadBuenEstado");
				version++;
				responder(intercambio, 200, articuloJson());
			}
		} else {
			responder(intercambio, 405, "{}");
		}
	}

	// Arma el JSON del articulo con el mismo formato que el inventario real
	private String articuloJson() {
		return "{\"id\":10,\"codigo\":\"BAL-1\",\"nombre\":\"Balon de futbol\",\"deporte\":\"Futbol\","
			+ "\"descripcion\":\"Balon oficial\",\"cantidadBuenEstado\":" + cantidadBuenEstado
			+ ",\"cantidadMantenimiento\":1,\"cantidadDanada\":2,\"cantidadTotal\":" + (cantidadBuenEstado + 3)
			+ ",\"version\":" + version + "}";
	}

	// Busca un campo numerico dentro del JSON recibido
	private long numeroDe(String json, String campo) {
		Matcher buscador = Pattern.compile("\"" + campo + "\"\\s*:\\s*(\\d+)").matcher(json);
		buscador.find();
		return Long.parseLong(buscador.group(1));
	}

	private void responder(HttpExchange intercambio, int codigo, String cuerpo) throws IOException {
		byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
		intercambio.getResponseHeaders().add("Content-Type", "application/json");
		intercambio.sendResponseHeaders(codigo, bytes.length);
		intercambio.getResponseBody().write(bytes);
		intercambio.close();
	}

}
