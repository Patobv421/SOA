package com.ejemplo.inventario.servicio;

import com.ejemplo.inventario.modelo.Articulo;
import com.ejemplo.inventario.modelo.ArticuloRequest;
import com.ejemplo.inventario.persistencia.ArticuloEntity;
import com.ejemplo.inventario.persistencia.ArticuloRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticuloService {
    
    private final ArticuloRepository repositorio;
    
    public ArticuloService(ArticuloRepository repositorio) {
        this.repositorio = repositorio;
    }
    
    @Transactional(readOnly = true)
    public List<Articulo> listar() {
        return repositorio.findAll(Sort.by("id"))
                .stream()
                .map(this::convertir)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Articulo consultar(Long id) {
        ArticuloEntity entidad = buscarPorId(id);
        return convertir(entidad);
    }
    
    @Transactional
    public Articulo crear(ArticuloRequest solicitud) {
        if (!Long.valueOf(0L).equals(solicitud.version())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Para crear un articulo, la version debe ser 0"
            );
        }
        
        ArticuloEntity entidad = new ArticuloEntity(
                solicitud.codigo().trim().toUpperCase(Locale.ROOT),
                solicitud.nombre().trim(),
                solicitud.deporte().trim(),
                solicitud.descripcion().trim(),
                solicitud.cantidadBuenEstado(),
                solicitud.cantidadMantenimiento(),
                solicitud.cantidadDanada()
        );
        
        ArticuloEntity guardado = repositorio.saveAndFlush(entidad);
        
        return convertir(guardado);
    }
    
    @Transactional
    public Articulo actualizar(Long id, ArticuloRequest solicitud) {
        ArticuloEntity entidad = buscarPorId(id);
        
        if (!Objects.equals(entidad.getVersion(), solicitud.version())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El articulo fue modificado. Consulta su version actual e intenta nuevamente"
            );
            
        }
        
        entidad.actualizar(
                solicitud.codigo().trim().toUpperCase(Locale.ROOT),
                solicitud.nombre().trim(),
                solicitud.deporte().trim(),
                solicitud.descripcion().trim(),
                solicitud.cantidadBuenEstado(),
                solicitud.cantidadMantenimiento(),
                solicitud.cantidadDanada()
        );
                
        ArticuloEntity guardado = repositorio.saveAndFlush(entidad);
        
        return convertir(guardado);
    }
    
    private ArticuloEntity buscarPorId(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser mayor que 0"
            );
        }
        
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un articulo con el identificador " + id
                ));
    }
    
    private Articulo convertir(ArticuloEntity entidad) {
        long cantidadTotal = entidad.getCantidadBuenEstado().longValue()
                + entidad.getCantidadMantenimiento().longValue()
                + entidad.getCantidadDanada().longValue();
        
        return new Articulo(
                entidad.getId(),
                entidad.getCodigo(),
                entidad.getNombre(),
                entidad.getDeporte(),
                entidad.getDescripcion(),
                entidad.getCantidadBuenEstado(),
                entidad.getCantidadMantenimiento(),
                entidad.getCantidadDanada(),
                cantidadTotal,
                entidad.getVersion()
        );
    }
            
}