package com.example.devoluciones_incidencias.servicio;

import com.example.devoluciones_incidencias.modelo.DevolucionRequest;
import com.example.devoluciones_incidencias.persistencia.RegistroIdempotenciaEntity;
import com.example.devoluciones_incidencias.persistencia.RegistroIdempotenciaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdempotenciaService {

    private final RegistroIdempotenciaRepository repositorio;

    public IdempotenciaService(RegistroIdempotenciaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public String calcularHuella(DevolucionRequest solicitud) {
        String contenido = String.format("%s|%s|%d|%s|%s",
                solicitud.idSolicitud(),
                solicitud.codigoArticulo().trim().toUpperCase(),
                solicitud.idUsuario(),
                solicitud.estadoMaterial().name(),
                solicitud.montoSancion().toPlainString()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error al calcular huella de idempotencia", e);
        }
    }

    @Transactional
    public Optional<String> verificarOIniciar(UUID claveIdempotencia, String huellaSolicitud) {
        Optional<RegistroIdempotenciaEntity> existente = repositorio.findById(claveIdempotencia);

        if (existente.isPresent()) {
            RegistroIdempotenciaEntity registro = existente.get();

            //Si el mismo UUID se intenta usar con datos distintos -> conflicto 409
            if (!registro.getHuellaSolicitud().equals(huellaSolicitud)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La clave de idempotencia (UUID) ya fue utilizada previamente con una solicitud diferente"
                );
            }

            //Si ya se completo previamente, recuperamos la respuesta guardada
            if ("COMPLETADO".equals(registro.getEstado())) {
                return Optional.ofNullable(registro.getRespuestaJson());
            }

            if ("PROCESANDO".equals(registro.getEstado())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La operacion con esta clave de idempotencia aun se encuentra en proceso"
                );
            }
        }

        //Registrar como PROCESANDO
        RegistroIdempotenciaEntity nuevo = new RegistroIdempotenciaEntity(
                claveIdempotencia,
                huellaSolicitud,
                "PROCESANDO",
                HttpStatus.CREATED.value(),
                null,
                Instant.now()
        );
        repositorio.save(nuevo);
        return Optional.empty();
    }

    @Transactional
    public void registrarResultado(UUID claveIdempotencia, int codigoHttp, String respuestaJson) {
        repositorio.findById(claveIdempotencia).ifPresent(registro -> {
            registro.marcarCompletado(codigoHttp, respuestaJson);
            repositorio.save(registro);
        });
    }

    @Transactional
    public void registrarFallo(UUID claveIdempotencia, int codigoHttp, String mensajeError) {
        repositorio.findById(claveIdempotencia).ifPresent(registro -> {
            registro.marcarFallido(codigoHttp, mensajeError);
            repositorio.save(registro);
        });
    }
}

