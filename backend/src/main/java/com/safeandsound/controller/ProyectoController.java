package com.safeandsound.controller;

import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.Rol;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;
    private final AutorizacionService autorizacionService;

    public ProyectoController(
            ProyectoService proyectoService,
            AutorizacionService autorizacionService
    ) {
        this.proyectoService = proyectoService;
        this.autorizacionService = autorizacionService;
    }

    @PostMapping
    public ResponseEntity<Proyecto> crearProyecto(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        Proyecto proyecto = proyectoService.crearProyecto(
                usuarioId,
                nombre,
                descripcion
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(proyecto);
    }

    @GetMapping("/{proyectoId}")
    public ResponseEntity<Proyecto> obtenerProyecto(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.VER_PROYECTO
        );

        return ResponseEntity.ok(
                proyectoService.obtenerProyecto(proyectoId)
        );
    }

    @PutMapping("/{proyectoId}")
    public ResponseEntity<Proyecto> modificarProyecto(
            @PathVariable Long proyectoId,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.MODIFICAR_PROYECTO
        );

        return ResponseEntity.ok(
                proyectoService.modificarProyecto(
                        proyectoId,
                        nombre,
                        descripcion
                )
        );
    }

    @DeleteMapping("/{proyectoId}")
    public ResponseEntity<Void> archivarProyecto(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.ARCHIVAR_PROYECTO
        );

        proyectoService.archivarProyecto(proyectoId);

        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{proyectoId}/miembros/{usuarioId}")
    public ResponseEntity<Void> agregarMiembro(
            @PathVariable Long proyectoId,
            @PathVariable Long usuarioId,
            @RequestParam Rol rol,
            Authentication authentication
    ) {
        Long usuarioActualId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(
                usuarioActualId,
                proyectoId,
                Operacion.MODIFICAR_ROLES
        );

        proyectoService.agregarMiembro(
                proyectoId,
                usuarioId,
                rol
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{proyectoId}/miembros/{usuarioId}/rol")
    public ResponseEntity<Void> modificarRol(
            @PathVariable Long proyectoId,
            @PathVariable Long usuarioId,
            @RequestParam Rol rol,
            Authentication authentication
    ) {
        Long usuarioActualId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(
                usuarioActualId,
                proyectoId,
                Operacion.MODIFICAR_ROLES
        );

        proyectoService.modificarRol(
                proyectoId,
                usuarioId,
                rol
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Proyecto>> listarProyectos(Authentication authentication) {
        Long usuarioId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                proyectoService.obtenerProyectosDelUsuario(usuarioId)
        );
    }
}