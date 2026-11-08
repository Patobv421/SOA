package com.proyecto.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.proyecto.solicitudes.modelo.ArticuloRespuesta;
import com.proyecto.solicitudes.modelo.UsuarioRespuesta;
import com.proyecto.solicitudes.repositorio.SolicitudRepositorio;
import com.proyecto.solicitudes.servicio.ClienteInventario;
import com.proyecto.solicitudes.servicio.ClienteUsuarios;
import com.proyecto.solicitudes.servicio.ErrorNegocioException;

// Pruebas del Servicio 2. Los servicios de Usuarios e Inventario se simulan con Mockito
@SpringBootTest
class SolicitudControladorTests {

	@Autowired
	private WebApplicationContext contexto;

	@Autowired
	private SolicitudRepositorio solicitudRepositorio;

	@MockitoBean
	private ClienteUsuarios clienteUsuarios;

	@MockitoBean
	private ClienteInventario clienteInventario;

	private MockMvc mockMvc;

	@BeforeEach
	void preparar() {
		mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
		solicitudRepositorio.deleteAll();

		// Por defecto el usuario 1 esta habilitado (limite 2) y el articulo 10 tiene 5 piezas
		when(clienteUsuarios.obtenerUsuario(1L)).thenReturn(crearUsuario(true, 2));
		when(clienteInventario.obtenerArticulo(10L)).thenReturn(crearArticulo(5));
	}

	// ---------- Casos exitosos ----------

	@Test
	void crear_solicitud_correcta() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(id, 1, 10, 2)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.idSolicitud").value(id.toString()))
			.andExpect(jsonPath("$.estado").value("PENDIENTE"));

		verify(clienteInventario, times(1)).reservar(any(), anyLong(), anyInt());
	}

	@Test
	void confirmar_solicitud_pendiente() throws Exception {
		UUID id = UUID.randomUUID();
		crearSolicitud(id, 1, 10, 1);

		mockMvc.perform(post("/api/solicitudes/" + id + "/confirmar"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CONFIRMADA"));
	}

	@Test
	void consultar_solicitud_existente() throws Exception {
		UUID id = UUID.randomUUID();
		crearSolicitud(id, 1, 10, 1);

		mockMvc.perform(get("/api/solicitudes/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cantidad").value(1));
	}

	@Test
	void cancelar_solicitud_pendiente_libera_la_reserva() throws Exception {
		UUID id = UUID.randomUUID();
		crearSolicitud(id, 1, 10, 2);

		mockMvc.perform(post("/api/solicitudes/" + id + "/cancelar"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CANCELADA"));

		verify(clienteInventario, times(1)).liberar(any(), anyLong(), anyInt());
	}

	// ---------- UUID y reintentos ----------

	@Test
	void repetir_el_mismo_uuid_con_los_mismos_datos_no_duplica() throws Exception {
		UUID id = UUID.randomUUID();
		crearSolicitud(id, 1, 10, 2);

		// Segundo intento (por ejemplo, se perdio la respuesta del primero)
		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(id, 1, 10, 2)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.idSolicitud").value(id.toString()));

		// Solo hay una solicitud y solo se reservo una vez
		assertEquals(1, solicitudRepositorio.count());
		verify(clienteInventario, times(1)).reservar(any(), anyLong(), anyInt());
	}

	@Test
	void reusar_el_uuid_con_otros_datos_da_conflicto() throws Exception {
		UUID id = UUID.randomUUID();
		crearSolicitud(id, 1, 10, 2);

		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(id, 1, 10, 3)))
			.andExpect(status().isConflict());
	}

	@Test
	void cancelar_dos_veces_no_libera_dos_veces() throws Exception {
		UUID id = UUID.randomUUID();
		crearSolicitud(id, 1, 10, 2);

		mockMvc.perform(post("/api/solicitudes/" + id + "/cancelar")).andExpect(status().isOk());
		mockMvc.perform(post("/api/solicitudes/" + id + "/cancelar"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CANCELADA"));

		verify(clienteInventario, times(1)).liberar(any(), anyLong(), anyInt());
	}

	// ---------- Casos invalidos ----------

	@Test
	void cantidad_cero_es_invalida() throws Exception {
		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(UUID.randomUUID(), 1, 10, 0)))
			.andExpect(status().isBadRequest());

		verify(clienteInventario, never()).reservar(any(), anyLong(), anyInt());
	}

	@Test
	void falta_el_uuid_de_la_operacion() throws Exception {
		String cuerpo = "{\"idUsuario\": 1, \"idArticulo\": 10, \"cantidad\": 1, \"fechaUso\": \"" + LocalDate.now().plusDays(1) + "\"}";

		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
			.andExpect(status().isBadRequest());
	}

	@Test
	void fecha_pasada_es_invalida() throws Exception {
		String cuerpo = "{\"idOperacion\": \"" + UUID.randomUUID() + "\", \"idUsuario\": 1, \"idArticulo\": 10, \"cantidad\": 1, \"fechaUso\": \"2020-01-01\"}";

		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
			.andExpect(status().isBadRequest());
	}

	@Test
	void consultar_solicitud_que_no_existe() throws Exception {
		mockMvc.perform(get("/api/solicitudes/" + UUID.randomUUID()))
			.andExpect(status().isNotFound());
	}

	// ---------- Reglas de negocio ----------

	@Test
	void usuario_deshabilitado_no_puede_solicitar() throws Exception {
		when(clienteUsuarios.obtenerUsuario(2L)).thenReturn(crearUsuario(false, 2));

		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(UUID.randomUUID(), 2, 10, 1)))
			.andExpect(status().isUnprocessableEntity());

		verify(clienteInventario, never()).reservar(any(), anyLong(), anyInt());
	}

	@Test
	void usuario_no_puede_pasar_su_limite_de_solicitudes() throws Exception {
		// El limite del usuario 1 es 2: las dos primeras si, la tercera no
		crearSolicitud(UUID.randomUUID(), 1, 10, 1);
		crearSolicitud(UUID.randomUUID(), 1, 10, 1);

		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(UUID.randomUUID(), 1, 10, 1)))
			.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void no_se_puede_pedir_mas_de_las_existencias() throws Exception {
		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(UUID.randomUUID(), 1, 10, 6)))
			.andExpect(status().isUnprocessableEntity());

		verify(clienteInventario, never()).reservar(any(), anyLong(), anyInt());
	}

	@Test
	void no_se_puede_cancelar_una_solicitud_confirmada() throws Exception {
		UUID id = UUID.randomUUID();
		crearSolicitud(id, 1, 10, 1);
		mockMvc.perform(post("/api/solicitudes/" + id + "/confirmar")).andExpect(status().isOk());

		mockMvc.perform(post("/api/solicitudes/" + id + "/cancelar"))
			.andExpect(status().isConflict());

		verify(clienteInventario, never()).liberar(any(), anyLong(), anyInt());
	}

	// ---------- Casos de falla ----------

	@Test
	void servicio_de_usuarios_apagado() throws Exception {
		when(clienteUsuarios.obtenerUsuario(1L))
			.thenThrow(new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de usuarios no esta disponible"));

		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(UUID.randomUUID(), 1, 10, 1)))
			.andExpect(status().isServiceUnavailable());

		assertEquals(0, solicitudRepositorio.count());
	}

	@Test
	void servicio_de_inventario_apagado_no_guarda_la_solicitud() throws Exception {
		doThrow(new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de inventario no esta disponible"))
			.when(clienteInventario).reservar(any(), anyLong(), anyInt());

		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(UUID.randomUUID(), 1, 10, 1)))
			.andExpect(status().isServiceUnavailable());

		assertEquals(0, solicitudRepositorio.count());
	}

	@Test
	void si_el_inventario_falla_al_cancelar_la_solicitud_sigue_pendiente() throws Exception {
		UUID id = UUID.randomUUID();
		crearSolicitud(id, 1, 10, 1);
		doThrow(new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de inventario no esta disponible"))
			.when(clienteInventario).liberar(any(), anyLong(), anyInt());

		mockMvc.perform(post("/api/solicitudes/" + id + "/cancelar"))
			.andExpect(status().isServiceUnavailable());

		mockMvc.perform(get("/api/solicitudes/" + id))
			.andExpect(jsonPath("$.estado").value("PENDIENTE"));
	}

	// ---------- Metodos de ayuda ----------

	// Crea una solicitud por el endpoint y comprueba que se creo
	private void crearSolicitud(UUID id, long idUsuario, long idArticulo, int cantidad) throws Exception {
		mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json(id, idUsuario, idArticulo, cantidad)))
			.andExpect(status().isCreated());
	}

	// Arma el JSON de la peticion
	private String json(UUID id, long idUsuario, long idArticulo, int cantidad) {
		return "{\"idOperacion\": \"" + id + "\", \"idUsuario\": " + idUsuario + ", \"idArticulo\": " + idArticulo
                        + ", \"cantidad\": " + cantidad + ", \"fechaUso\": \"" + LocalDate.now().plusDays(1) + "\"}";
	}

	private UsuarioRespuesta crearUsuario(boolean habilitado, int limite) {
		UsuarioRespuesta usuario = new UsuarioRespuesta();
		usuario.setId(1L);
		usuario.setNombre("Alumno de prueba");
		usuario.setHabilitado(habilitado);
		usuario.setLimiteSolicitudes(limite);
		return usuario;
	}

	private ArticuloRespuesta crearArticulo(int disponibles) {
		ArticuloRespuesta articulo = new ArticuloRespuesta();
		articulo.setId(10L);
		articulo.setNombre("Balon de futbol");
		articulo.setCantidadDisponible(disponibles);
		return articulo;
	}

}
