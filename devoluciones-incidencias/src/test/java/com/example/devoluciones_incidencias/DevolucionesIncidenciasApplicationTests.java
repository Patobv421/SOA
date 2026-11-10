package com.example.devoluciones_incidencias;

import com.example.devoluciones_incidencias.modelo.EstadoMaterial;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DevolucionesIncidenciasApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void verificarEstadosMaterial() {
		// Validar estados del material
		assertNotNull(EstadoMaterial.BUEN_ESTADO);
		assertNotNull(EstadoMaterial.REQUIERE_MANTENIMIENTO);
		assertNotNull(EstadoMaterial.DANADO_IRREPARABLE);
	}

}
