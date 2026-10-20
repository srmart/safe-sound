
package com.safeandsound.controller;

import com.safeandsound.dto.AuditoriaResponse;
import com.safeandsound.dto.ProyectoListadoDTO;
import com.safeandsound.dto.ProyectoResponse;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.RegistroAuditoria;
import com.safeandsound.model.Rol;
import com.safeandsound.service.AuditService;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;
import com.safeandsound.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.safeandsound.dto.ProyectoMiembroResponse;

import java.util.List;

@RestController
@RequestMapping("/proyectos")
@PreAuthorize("isAuthenticated()")
public class ProyectoController {

    private final ProyectoService proyectoService;
    private final AutorizacionService autorizacionService;
    private final AuditService auditService;
    private final UsuarioService usuarioService;

    public ProyectoController(
            ProyectoService proyectoService,
            AutorizacionService autorizacionService,
            AuditService auditService,
            UsuarioService usuarioService
    ) {
        this.proyectoService = proyectoService;
        this.autorizacionService = autorizacionService;
        this.auditService = auditService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<ProyectoResponse> crearProyecto(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        Proyecto proyecto = proyectoService.crearProyecto(
                usuarioId, nombre, descripcion
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProyectoResponse.desde(proyecto));
    }

    @GetMapping("/{proyectoId}")
    public ResponseEntity<ProyectoResponse> obtenerProyecto(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(
                usuarioId, proyectoId, Operacion.VER_PROYECTO
        );

        Proyecto proyecto = proyectoService.obtenerProyecto(proyectoId);

        return ResponseEntity.ok(ProyectoResponse.desde(proyecto));
    }

    @PutMapping("/{proyectoId}")
    public ResponseEntity<ProyectoResponse> modificarProyecto(
            @PathVariable Long proyectoId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam String passwordConfirmacion,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        // RS16: Debe enviarse al menos un campo para modificar.
        if (nombre == null && descripcion == null) {
            return ResponseEntity.badRequest().build();
        }

        // RS19: Reautenticación antes de modificar el proyecto.
        usuarioService.verificarPassword(
                usuarioId,
                passwordConfirmacion.toCharArray()
        );

        // RS16: Autorización diferenciada por campo.
        if (nombre != null) {
            autorizacionService.verificarPermiso(
                    usuarioId,
                    proyectoId,
                    Operacion.MODIFICAR_NOMBRE_PROYECTO
            );
        }

        if (descripcion != null) {
            autorizacionService.verificarPermiso(
                    usuarioId,
                    proyectoId,
                    Operacion.MODIFICAR_DESCRIPCION_PROYECTO
            );
        }

        // RS13: El servicio verifica que el proyecto esté activo.
        Proyecto proyecto = proyectoService.modificarProyecto(
                proyectoId,
                nombre,
                descripcion
        );

        // RS19: Registrar únicamente los campos modificados.
        String detalles = "";

        if (nombre != null) {
            detalles += "Nombre modificado: " + nombre + ". ";
        }

        if (descripcion != null) {
            detalles += "Descripción modificada: " + descripcion;
        }

        auditService.registrarAccionAdministrativa(
                usuarioId,
                proyectoId,
                "MODIFICAR_PROYECTO",
                detalles
        );

        return ResponseEntity.ok(ProyectoResponse.desde(proyecto));
    }

    @DeleteMapping("/{proyectoId}")
    public ResponseEntity<Void> archivarProyecto(
            @PathVariable Long proyectoId,
            @RequestParam String passwordConfirmacion,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        // RS19: Reautenticación.
        usuarioService.verificarPassword(
                usuarioId,
                passwordConfirmacion.toCharArray()
        );

        // Verificar permiso para archivar.
        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.ARCHIVAR_PROYECTO
        );

        // RS13: Archivar el proyecto.
        proyectoService.archivarProyecto(proyectoId);

        // RS19: Registrar la operación en auditoría.
        auditService.registrarAccionAdministrativa(
                usuarioId,
                proyectoId,
                "ARCHIVAR_PROYECTO",
                "Proyecto archivado"
        );

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{proyectoId}/miembros")
    public ResponseEntity<List<ProyectoMiembroResponse>> listarMiembros(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioActualId = (Long) authentication.getPrincipal();

        // Verificar que el usuario tenga permiso para ver miembros.
        autorizacionService.verificarPermiso(
                usuarioActualId,
                proyectoId,
                Operacion.VER_MIEMBROS
        );

        List<ProyectoMiembroResponse> miembros =
                proyectoService.listarMiembros(proyectoId)
                        .stream()
                        .map(ProyectoMiembroResponse::desde)
                        .toList();

        return ResponseEntity.ok(miembros);
    }


    @PostMapping("/{proyectoId}/miembros/{usuarioId}")
    public ResponseEntity<Void> agregarMiembro(
            @PathVariable Long proyectoId,
            @PathVariable Long usuarioId,
            @RequestParam Rol rol,
            @RequestParam String passwordConfirmacion,
            Authentication authentication
    ) {
        Long usuarioActualId = (Long) authentication.getPrincipal();

        // RS19: Reautenticación.
        usuarioService.verificarPassword(
                usuarioActualId,
                passwordConfirmacion.toCharArray()
        );

        // Verificar permisos administrativos.
        autorizacionService.verificarPermiso(
                usuarioActualId,
                proyectoId,
                Operacion.MODIFICAR_ROLES
        );

        proyectoService.agregarMiembro(proyectoId, usuarioId, rol);

        // RS19: Auditoría.
        auditService.registrarAccionAdministrativa(
                usuarioActualId,
                proyectoId,
                "AGREGAR_MIEMBRO",
                "Usuario ID: " + usuarioId + ", Rol asignado: " + rol
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{proyectoId}/auditoria")
    public ResponseEntity<List<AuditoriaResponse>> obtenerAuditoria(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        // RS19: Solo el OWNER puede consultar la auditoría.
        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.VER_AUDITORIA
        );

        List<RegistroAuditoria> registros =
                auditService.obtenerAuditoriaProyecto(proyectoId);

        List<AuditoriaResponse> response = registros.stream()
                .map(r -> new AuditoriaResponse(
                        r.getId(),
                        r.getUsuarioId(),
                        r.getProyectoId(),
                        r.getOperacion(),
                        r.getDetalles(),
                        r.getFechaHora()
                ))
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

        List<ProyectoListadoDTO> proyectos =
                proyectoService.listarProyectos(usuarioId, nombre, activo)
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


    @PutMapping("/{proyectoId}/miembros/{usuarioId}/rol")
    public ResponseEntity<Void> modificarRol(
            @PathVariable Long proyectoId,
            @PathVariable Long usuarioId,
            @RequestParam Rol rol,
            @RequestParam String passwordConfirmacion,
            Authentication authentication
    ) {
        Long usuarioActualId = (Long) authentication.getPrincipal();

        // RS19: Reautenticación antes de modificar roles.
        usuarioService.verificarPassword(
                usuarioActualId,
                passwordConfirmacion.toCharArray()
        );

        // Solo el OWNER puede modificar roles.
        autorizacionService.verificarPermiso(
                usuarioActualId,
                proyectoId,
                Operacion.MODIFICAR_ROLES
        );

        proyectoService.modificarRol(proyectoId, usuarioId, rol);

        // RS19: Registrar la operación administrativa.
        auditService.registrarAccionAdministrativa(
                usuarioActualId,
                proyectoId,
                "MODIFICAR_ROL",
                "Usuario ID: " + usuarioId + ", nuevo rol: " + rol
        );

        return ResponseEntity.noContent().build();
    }

}
