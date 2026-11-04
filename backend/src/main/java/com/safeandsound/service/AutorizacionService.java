
package com.safeandsound.service;

import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.repository.ProyectoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AutorizacionService {

    private final ProyectoMiembroRepository proyectoMiembroRepository;
    private final ProyectoRepository proyectoRepository;

    public AutorizacionService(
            ProyectoMiembroRepository proyectoMiembroRepository,
            ProyectoRepository proyectoRepository
    ) {
        this.proyectoMiembroRepository = proyectoMiembroRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public void verificarPermiso(
            Long usuarioId,
            Long proyectoId,
            Operacion operacion
    ) {
        ProyectoMiembro miembro;

        try {
            miembro = proyectoMiembroRepository
                    .findByProyectoIdAndUsuarioId(proyectoId, usuarioId)
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "El usuario no pertenece al proyecto"
                            )
                    );
        } catch (AccessDeniedException e) {
            // RS9: Mantener la denegación de acceso.
            throw e;
        } catch (Exception e) {
            // RS9: Ante errores de autorización, denegar acceso.
            throw new AccessDeniedException(
                    "No se pudo verificar la autorización del usuario"
            );
        }

        Rol rol = miembro.getRol();

        if (!tienePermiso(rol, operacion)) {
            throw new AccessDeniedException(
                    "El rol " + rol + " no tiene permiso para " + operacion
            );
        }

        // INC-9 / RS13:
        // Las operaciones de escritura requieren un proyecto activo.
        if (esOperacionDeEscritura(operacion)) {
            verificarProyectoActivo(proyectoId);
        }
    }

    // INC-9 / RS13: Validación del estado del recurso.
    public void verificarProyectoActivo(Long proyectoId) {
        Proyecto proyecto;

        try {
            proyecto = proyectoRepository.findById(proyectoId)
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "No se pudo verificar el estado del proyecto"
                            )
                    );
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            // RS9: Fallar de forma segura.
            throw new AccessDeniedException(
                    "No se pudo verificar el estado del proyecto"
            );
        }

        if (!proyecto.isActivo()) {
            throw new AccessDeniedException(
                    "No se permiten modificaciones en un proyecto archivado"
            );
        }
    }

    // INC-9: Clasificación explícita de operaciones de escritura.
    private boolean esOperacionDeEscritura(Operacion operacion) {
        return switch (operacion) {
            case MODIFICAR_PROYECTO,
                 MODIFICAR_NOMBRE_PROYECTO,
                 MODIFICAR_DESCRIPCION_PROYECTO,
                 ARCHIVAR_PROYECTO,
                 AGREGAR_MIEMBRO,
                 ELIMINAR_MIEMBRO,
                 MODIFICAR_ROLES,
                 SUBIR_ARCHIVO,
                 ELIMINAR_ARCHIVO,
                 SOLICITAR_RESTAURACION,
                 APROBAR_SOLICITUD_RESTAURACION,
                 RECHAZAR_SOLICITUD_RESTAURACION,
                 RESTAURAR_ARCHIVO -> true;

            default -> false;
        };
    }

    private boolean tienePermiso(Rol rol, Operacion operacion) {
        // RS6: Denegar por defecto si falta información.
        if (rol == null || operacion == null) {
            return false;
        }

        // RS20: Matriz explícita de autorización.
        return switch (operacion) {

            // --- Operaciones de Proyecto ---
            case VER_PROYECTO ->
                    rol == Rol.OWNER
                            || rol == Rol.COLLABORATOR
                            || rol == Rol.VIEWER;

            case MODIFICAR_PROYECTO ->
                    rol == Rol.OWNER;

            case ARCHIVAR_PROYECTO ->
                    rol == Rol.OWNER;

            // --- Operaciones de Miembros ---
            case VER_MIEMBROS ->
                    rol == Rol.OWNER
                            || rol == Rol.COLLABORATOR
                            || rol == Rol.VIEWER;

            case AGREGAR_MIEMBRO ->
                    rol == Rol.OWNER;

            case ELIMINAR_MIEMBRO ->
                    rol == Rol.OWNER;

            case MODIFICAR_ROLES ->
                    rol == Rol.OWNER;

            // --- Operaciones de Archivos ---
            case VER_ARCHIVOS ->
                    rol == Rol.OWNER
                            || rol == Rol.COLLABORATOR
                            || rol == Rol.VIEWER;

            case SUBIR_ARCHIVO ->
                    rol == Rol.OWNER
                            || rol == Rol.COLLABORATOR;

            case DESCARGAR_ARCHIVO ->
                    rol == Rol.OWNER
                            || rol == Rol.COLLABORATOR
                            || rol == Rol.VIEWER;

            case ELIMINAR_ARCHIVO ->
                    rol == Rol.OWNER
                            || rol == Rol.COLLABORATOR;

            // --- Operaciones de restauración JIT ---
            case SOLICITAR_RESTAURACION ->
                    rol == Rol.COLLABORATOR;

            case APROBAR_SOLICITUD_RESTAURACION,
                 RECHAZAR_SOLICITUD_RESTAURACION ->
                    rol == Rol.OWNER;

            // La restauración requiere autorización JIT vigente.
            // No se concede por el rol permanente.
            case RESTAURAR_ARCHIVO ->
                    false;

            // --- Operaciones Administrativas (RS19) ---
            case VER_AUDITORIA ->
                    rol == Rol.OWNER;

            // --- RS16: Permisos por campo ---
            case MODIFICAR_NOMBRE_PROYECTO ->
                    rol == Rol.OWNER;

            case MODIFICAR_DESCRIPCION_PROYECTO ->
                    rol == Rol.OWNER
                            || rol == Rol.COLLABORATOR;

            // RS20 / RS6: Denegar operaciones no contempladas.
            default -> false;
        };
    }
}
