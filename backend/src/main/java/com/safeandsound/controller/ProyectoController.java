package com.safeandsound.controller;

import com.safeandsound.dto.AuditoriaResponse;
import com.safeandsound.dto.ProyectoListadoDTO;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.dto.ProyectoResponse;
import com.safeandsound.model.RegistroAuditoria;
import com.safeandsound.model.Rol;
import com.safeandsound.service.AuditService;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proyectos")
@PreAuthorize("isAuthenticated()")
public class ProyectoController {

    private final ProyectoService proyectoService;
    private final AutorizacionService autorizacionService;
    private final AuditService auditService; // <-- AGREGADO PARA RS19

    public ProyectoController(
            ProyectoService proyectoService,
            AutorizacionService autorizacionService,
            AuditService auditService // <-- AGREGADO PARA RS19
    ) {
        this.proyectoService = proyectoService;
        this.autorizacionService = autorizacionService;
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<ProyectoResponse> crearProyecto(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        Proyecto proyecto = proyectoService.crearProyecto(usuarioId, nombre, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProyectoResponse.desde(proyecto));
    }

    @GetMapping("/{proyectoId}")
    public ResponseEntity<ProyectoResponse> obtenerProyecto(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.VER_PROYECTO);
        Proyecto proyecto = proyectoService.obtenerProyecto(proyectoId);
        return ResponseEntity.ok(ProyectoResponse.desde(proyecto));
    }

    @PutMapping("/{proyectoId}")
    public ResponseEntity<ProyectoResponse> modificarProyecto(
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

        // RS13: El servicio verifica que el proyecto esté activo.
        Proyecto proyecto = proyectoService.modificarProyecto(
                proyectoId,
                nombre,
                descripcion
        );

        // RS19: Registrar la acción después de modificar el proyecto.
        auditService.registrarAccionAdministrativa(
                usuarioId,
                proyectoId,
                "MODIFICAR_PROYECTO",
                "Nombre: " + nombre + ", Descripción: " + descripcion
        );

        return ResponseEntity.ok(ProyectoResponse.desde(proyecto));
    }

    @DeleteMapping("/{proyectoId}")
    public ResponseEntity<Void> archivarProyecto(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.ARCHIVAR_PROYECTO);
        
        // RS19: Auditoría de acción administrativa de alto riesgo
        auditService.registrarAccionAdministrativa(usuarioId, proyectoId, "ARCHIVAR_PROYECTO", "Proyecto archivado");

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
        autorizacionService.verificarPermiso(usuarioActualId, proyectoId, Operacion.MODIFICAR_ROLES);
        
        // RS19: Auditoría de acción administrativa
        auditService.registrarAccionAdministrativa(usuarioActualId, proyectoId, "AGREGAR_MIEMBRO", 
                "Usuario ID: " + usuarioId + ", Rol asignado: " + rol);

        proyectoService.agregarMiembro(proyectoId, usuarioId, rol);
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
        autorizacionService.verificarPermiso(usuarioActualId, proyectoId, Operacion.MODIFICAR_ROLES);
        
        // RS19: Auditoría de acción administrativa
        auditService.registrarAccionAdministrativa(usuarioActualId, proyectoId, "MODIFICAR_ROL", 
                "Usuario ID: " + usuarioId + ", Nuevo rol: " + rol);

        proyectoService.modificarRol(proyectoId, usuarioId, rol);
        return ResponseEntity.noContent().build();
    }

    // RS19: Nuevo endpoint para consultar el historial de acciones administrativas
    @GetMapping("/{proyectoId}/auditoria")
    public ResponseEntity<List<AuditoriaResponse>> obtenerAuditoria(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        
        // RS19: Control adicional: solo el OWNER puede consultar la auditoría del proyecto
        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.VER_AUDITORIA);

        List<RegistroAuditoria> registros = auditService.obtenerAuditoriaProyecto(proyectoId);
        
        List<AuditoriaResponse> response = registros.stream()
                .map(r -> new AuditoriaResponse(r.getId(), r.getUsuarioId(), r.getProyectoId(), r.getOperacion(), r.getDetalles(), r.getFechaHora()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProyectoListadoDTO>> listarProyectos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        List<ProyectoListadoDTO> proyectos = proyectoService.listarProyectos(usuarioId, nombre, activo)
                .stream()
                .map(miembro -> new ProyectoListadoDTO(
                        miembro.getProyecto().getId(),
                        miembro.getProyecto().getNombre(),
                        miembro.getProyecto().getDescripcion(),
                        miembro.getProyecto().isActivo(),
                        miembro.getRol()
                ))
                .toList();
        return ResponseEntity.ok(proyectos);
    }
}