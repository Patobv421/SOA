package com.example.devoluciones_incidencias.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotencia_devoluciones", schema = "public")
public class RegistroIdempotenciaEntity {

    @Id
    @Column(name = "clave_idempotencia", nullable = false)
    private UUID claveIdempotencia;

    @Column(name = "huella_solicitud", nullable = false, length = 64)
    private String huellaSolicitud;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "codigo_estado_http", nullable = false)
    private int codigoEstadoHttp;

    @Column(name = "respuesta_json", columnDefinition = "TEXT")
    private String respuestaJson;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;

    protected RegistroIdempotenciaEntity() {
    }

    public RegistroIdempotenciaEntity(
            UUID claveIdempotencia,
            String huellaSolicitud,
            String estado,
            int codigoEstadoHttp,
            String respuestaJson,
            Instant fechaRegistro
    ) {
        this.claveIdempotencia = claveIdempotencia;
        this.huellaSolicitud = huellaSolicitud;
        this.estado = estado;
        this.codigoEstadoHttp = codigoEstadoHttp;
        this.respuestaJson = respuestaJson;
        this.fechaRegistro = fechaRegistro;
    }

    public void marcarCompletado(int codigoEstadoHttp, String respuestaJson) {
        this.estado = "COMPLETADO";
        this.codigoEstadoHttp = codigoEstadoHttp;
        this.respuestaJson = respuestaJson;
    }

    public void marcarFallido(int codigoEstadoHttp, String respuestaJson) {
        this.estado = "FALLIDO";
        this.codigoEstadoHttp = codigoEstadoHttp;
        this.respuestaJson = respuestaJson;
    }

    public UUID getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public String getHuellaSolicitud() {
        return huellaSolicitud;
    }

    public String getEstado() {
        return estado;
    }

    public int getCodigoEstadoHttp() {
        return codigoEstadoHttp;
    }

    public String getRespuestaJson() {
        return respuestaJson;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }
}

