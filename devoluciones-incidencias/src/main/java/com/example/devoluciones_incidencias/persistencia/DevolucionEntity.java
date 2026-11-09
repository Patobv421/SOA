package com.example.devoluciones_incidencias.persistencia;

import com.example.devoluciones_incidencias.modelo.EstadoMaterial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "devoluciones", schema = "public")
public class DevolucionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String folio;

    @Column(name = "id_solicitud", nullable = false, length = 64)
    private String idSolicitud;

    @Column(name = "codigo_articulo", nullable = false, length = 30)
    private String codigoArticulo;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "fecha_devolucion", nullable = false)
    private Instant fechaDevolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_material", nullable = false, length = 30)
    private EstadoMaterial estadoMaterial;

    @Column(nullable = false, length = 500)
    private String observaciones;

    @Column(name = "monto_sancion", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoSancion;

    @Version
    @Column(nullable = false)
    private Long version;

    protected DevolucionEntity() {
    }

    public DevolucionEntity(
            String folio,
            String idSolicitud,
            String codigoArticulo,
            Long idUsuario,
            Instant fechaDevolucion,
            EstadoMaterial estadoMaterial,
            String observaciones,
            BigDecimal montoSancion
    ) {
        this.folio = folio;
        this.idSolicitud = idSolicitud;
        this.codigoArticulo = codigoArticulo;
        this.idUsuario = idUsuario;
        this.fechaDevolucion = fechaDevolucion;
        this.estadoMaterial = estadoMaterial;
        this.observaciones = observaciones;
        this.montoSancion = montoSancion;
    }

    public void actualizarObservaciones(String nuevasObservaciones) {
        this.observaciones = nuevasObservaciones;
    }

    public Long getId() {
        return id;
    }

    public String getFolio() {
        return folio;
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public String getCodigoArticulo() {
        return codigoArticulo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public Instant getFechaDevolucion() {
        return fechaDevolucion;
    }

    public EstadoMaterial getEstadoMaterial() {
        return estadoMaterial;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public BigDecimal getMontoSancion() {
        return montoSancion;
    }

    public Long getVersion() {
        return version;
    }
}

