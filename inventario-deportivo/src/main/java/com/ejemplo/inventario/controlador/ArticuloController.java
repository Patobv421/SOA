package com.ejemplo.inventario.controlador;

import com.ejemplo.inventario.modelo.Articulo;
import com.ejemplo.inventario.modelo.ArticuloRequest;
import com.ejemplo.inventario.servicio.ArticuloService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping("/api/articulos")
public class ArticuloController {
        
        private final ArticuloService servicio;
        
        public ArticuloController(ArticuloService servicio) {
            this.servicio = servicio;
        }
        
        @GetMapping
        public List<Articulo> listar() {
            return servicio.listar();
        }
        
        @GetMapping("/{id}")
        public Articulo consultar(@PathVariable("id") Long id) {
            return servicio.consultar(id);
        }
        
        @PostMapping
        public ResponseEntity<Articulo> crear(
            @Valid @RequestBody ArticuloRequest solicitud
        ) {
            Articulo creado = servicio.crear(solicitud);
            
            URI ubicacion = URI.create("/api/articulos/" + creado.id());
            
            return ResponseEntity.created(ubicacion).body(creado);
        }
        
        @PutMapping("/{id}")
        public Articulo actualizar(
                @PathVariable("id") Long id,
                @Valid @RequestBody ArticuloRequest solicitud
        ) {
            return servicio.actualizar(id, solicitud);
        }
}