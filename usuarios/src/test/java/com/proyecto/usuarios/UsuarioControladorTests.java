package com.proyecto.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.proyecto.usuarios.repositorio.IncidenciaRepositorio;
import com.proyecto.usuarios.repositorio.UsuarioRepositorio;

// Pruebas del Servicio 3 (Usuarios) con una base H2 en memoria
@SpringBootTest
class UsuarioControladorTests {

	@Autowired
	private WebApplicationContext contexto;

	@Autowired
	private UsuarioRepositorio usuarioRepositorio;

	@Autowired
	private IncidenciaRepositorio incidenciaRepositorio;

	private MockMvc mockMvc;

	@BeforeEach
	void preparar() {
		mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
		incidenciaRepositorio.deleteAll();
		usuarioRepositorio.deleteAll();
	}

	// ---------- Casos exitosos ----------

	@Test
	void registrar_alumno_correcto() throws Exception {
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(jsonUsuario("a0012345", "ana@uni.mx", "ALUMNO", LocalDate.now().plusYears(1))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.matricula").value("A0012345"))
			.andExpect(jsonPath("$.estado").value("ACTIVO"))
			.andExpect(jsonPath("$.habilitado").value(true))
			.andExpect(jsonPath("$.limiteSolicitudes").value(2));
	}

	@Test
	void consulta_tiene_los_campos_que_usa_solicitudes() throws Exception {
		long id = crearUsuario("D0000001", "DOCENTE");

		mockMvc.perform(get("/api/usuarios/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.nombre").exists())
			.andExpect(jsonPath("$.habilitado").value(true))
			.andExpect(jsonPath("$.limiteSolicitudes").value(4));
	}

	@Test
	void buscar_por_matricula() throws Exception {
		crearUsuario("A0000002", "ALUMNO");

		mockMvc.perform(get("/api/usuarios/matricula/a0000002"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.matricula").value("A0000002"));
	}

	// ---------- Datos invalidos ----------

	@Test
	void datos_invalidos_regresan_400() throws Exception {
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"matricula\": \"12\", \"nombre\": \"\", \"correo\": \"nocorreo\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.codigo").value(400));
	}

	@Test
	void json_mal_escrito_regresa_400() throws Exception {
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{matricula"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void usuario_que_no_existe_regresa_404() throws Exception {
		mockMvc.perform(get("/api/usuarios/9999"))
			.andExpect(status().isNotFound());
	}

	@Test
	void id_con_letras_regresa_400() throws Exception {
		mockMvc.perform(get("/api/usuarios/abc"))
			.andExpect(status().isBadRequest());
	}

	// ---------- Reglas de negocio ----------

	@Test
	void matricula_repetida_regresa_409() throws Exception {
		crearUsuario("A0000003", "ALUMNO");

		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(jsonUsuario("A0000003", "otro@uni.mx", "ALUMNO", LocalDate.now().plusYears(1))))
			.andExpect(status().isConflict());
	}

	@Test
	void credencial_vencida_no_se_puede_registrar() throws Exception {
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(jsonUsuario("A0000004", "vencida@uni.mx", "ALUMNO", LocalDate.now().minusDays(1))))
			.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void credencial_que_vence_deja_al_usuario_no_habilitado() throws Exception {
		long id = crearUsuario("A0000005", "ALUMNO");

		// Se actualiza con una credencial que ya vencio
		mockMvc.perform(put("/api/usuarios/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(jsonUsuario("A0000005", "a0000005@uni.mx", "ALUMNO", LocalDate.now().minusDays(1))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.habilitado").value(false));
	}

	@Test
	void usuario_dado_de_baja_no_esta_habilitado() throws Exception {
		long id = crearUsuario("A0000006", "ALUMNO");

		mockMvc.perform(patch("/api/usuarios/" + id + "/estado").contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\": \"BAJA\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("BAJA"))
			.andExpect(jsonPath("$.habilitado").value(false));
	}

	@Test
	void no_se_puede_suspender_a_mano() throws Exception {
		long id = crearUsuario("A0000007", "ALUMNO");

		mockMvc.perform(patch("/api/usuarios/" + id + "/estado").contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\": \"SUSPENDIDO\"}"))
			.andExpect(status().isUnprocessableEntity());
	}

	// ---------- Incidencias (vienen de Devoluciones) ----------

	@Test
	void incidencia_grave_suspende_al_usuario() throws Exception {
		long id = crearUsuario("A0000008", "ALUMNO");

		mockMvc.perform(post("/api/usuarios/" + id + "/incidencias").contentType(MediaType.APPLICATION_JSON)
				.content(jsonIncidencia(UUID.randomUUID(), "GRAVE")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estadoUsuario").value("SUSPENDIDO"));

		// Ahora Solicitudes vera que no esta habilitado
		mockMvc.perform(get("/api/usuarios/" + id))
			.andExpect(jsonPath("$.habilitado").value(false));

		// Y no se puede reactivar antes de tiempo
		mockMvc.perform(patch("/api/usuarios/" + id + "/estado").contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\": \"ACTIVO\"}"))
			.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void tres_incidencias_leves_suspenden() throws Exception {
		long id = crearUsuario("A0000009", "ALUMNO");

		for (int i = 1; i <= 2; i++) {
			mockMvc.perform(post("/api/usuarios/" + id + "/incidencias").contentType(MediaType.APPLICATION_JSON)
					.content(jsonIncidencia(UUID.randomUUID(), "LEVE")))
				.andExpect(jsonPath("$.estadoUsuario").value("ACTIVO"));
		}
		mockMvc.perform(post("/api/usuarios/" + id + "/incidencias").contentType(MediaType.APPLICATION_JSON)
				.content(jsonIncidencia(UUID.randomUUID(), "LEVE")))
			.andExpect(jsonPath("$.estadoUsuario").value("SUSPENDIDO"));
	}

	@Test
	void misma_incidencia_repetida_no_se_duplica() throws Exception {
		long id = crearUsuario("A0000010", "ALUMNO");
		UUID op = UUID.randomUUID();

		mockMvc.perform(post("/api/usuarios/" + id + "/incidencias").contentType(MediaType.APPLICATION_JSON)
				.content(jsonIncidencia(op, "LEVE")))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/usuarios/" + id + "/incidencias").contentType(MediaType.APPLICATION_JSON)
				.content(jsonIncidencia(op, "LEVE")))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/usuarios/" + id + "/incidencias"))
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void mismo_uuid_con_otros_datos_regresa_409() throws Exception {
		long id = crearUsuario("A0000011", "ALUMNO");
		UUID op = UUID.randomUUID();

		mockMvc.perform(post("/api/usuarios/" + id + "/incidencias").contentType(MediaType.APPLICATION_JSON)
				.content(jsonIncidencia(op, "LEVE")))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/usuarios/" + id + "/incidencias").contentType(MediaType.APPLICATION_JSON)
				.content(jsonIncidencia(op, "GRAVE")))
			.andExpect(status().isConflict());
	}

	// ---------- Ayudas ----------

	private long crearUsuario(String matricula, String tipo) throws Exception {
		String respuesta = mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(jsonUsuario(matricula, matricula.toLowerCase() + "@uni.mx", tipo, LocalDate.now().plusYears(1))))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		Number id = JsonPath.read(respuesta, "$.id");
		return id.longValue();
	}

	private String jsonUsuario(String matricula, String correo, String tipo, LocalDate vigencia) {
		return "{\"matricula\": \"" + matricula + "\", \"nombre\": \"Usuario " + matricula + "\", \"correo\": \""
			+ correo + "\", \"tipo\": \"" + tipo + "\", \"vigenciaCredencial\": \"" + vigencia + "\"}";
	}

	private String jsonIncidencia(UUID idOperacion, String gravedad) {
		return "{\"idOperacion\": \"" + idOperacion + "\", \"gravedad\": \"" + gravedad
			+ "\", \"descripcion\": \"Prueba de incidencia\"}";
	}

}
