package com.ejemplo.inventario.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "articulos", schema = "public")
public class ArticuloEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique =true, length = 30)
    private String codigo;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(nullable = false, length = 60)
    private String deporte;
    
    @Column(nullable = false, length = 500)
    private String descripcion;
    
    @Column(name = "cantidad_buen_estado", nullable = false)
    private Integer cantidadBuenEstado;
    
    @Column(name = "cantidad_mantenimiento", nullable = false)
    private Integer cantidadMantenimiento;
    
    @Column(name = "cantidad_danada", nullable = false)
    private Integer cantidadDanada;
    
    @Version
    @Column(nullable = false)
    private Long version;
    
    protected ArticuloEntity() {
        
    }
    
    public ArticuloEntity(
            String codigo,
            String nombre,
            String deporte,
            String descripcion,
            Integer cantidadBuenEstado,
            Integer cantidadMantenimiento,
            Integer cantidadDanada
    ) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.deporte = deporte;
        this.descripcion = descripcion;
        this.cantidadBuenEstado = cantidadBuenEstado;
        this.cantidadMantenimiento = cantidadMantenimiento;
        this.cantidadDanada = cantidadDanada;
    }
    
    public void actualizar(
            String codigo,
            String nombre,
            String deporte,
            String descripcion,
            Integer cantidadBuenEstado,
            Integer cantidadMantenimiento,
            Integer cantidadDanada
    ) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.deporte = deporte;
        this.descripcion = descripcion;
        this.cantidadBuenEstado = cantidadBuenEstado;
        this.cantidadMantenimiento = cantidadMantenimiento;
        this.cantidadDanada = cantidadDanada;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public String getDeporte() {
        return deporte;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public Integer getCantidadBuenEstado() {
        return cantidadBuenEstado;
    }
    
    public Integer getCantidadMantenimiento() {
        return cantidadMantenimiento;
    }
    
    public Integer getCantidadDanada() {
        return cantidadDanada;
    }
    
    public Long getVersion() {
        return version;
    }
}
