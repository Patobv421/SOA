package com.proyecto.solicitudes.servicio;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Crea el RestClient que usamos para llamar a los otros servicios
// Aqui se pone el tiempo de espera para que no se quede esperando para siempre
public class ConfiguracionRestClient {

	public static RestClient crear(String urlBase, int segundosEspera) {
		HttpClient httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(segundosEspera))
			.build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
		fabrica.setReadTimeout(Duration.ofSeconds(segundosEspera));

		return RestClient.builder()
			.baseUrl(urlBase)
			.requestFactory(fabrica)
			.build();
	}

}
