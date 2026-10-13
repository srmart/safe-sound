package com.safeandsound.service;

import com.safeandsound.model.Operacion;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.repository.ProyectoMiembroRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AutorizacionService {

    private final ProyectoMiembroRepository proyectoMiembroRepository;

    public AutorizacionService(ProyectoMiembroRepository proyectoMiembroRepository) {
        this.proyectoMiembroRepository = proyectoMiembroRepository;
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
            // Si el acceso ya fue denegado, mantenemos la denegación.
            throw e;
        } catch (Exception e) {
            // RS9: ante cualquier error durante la comprobación
            // de autorización, se deniega el acceso.
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
    }

    private boolean tienePermiso(Rol rol, Operacion operacion) {
        // RS6: si falta información necesaria para autorizar,
        // el acceso se deniega por defecto.
        if (rol == null || operacion == null) {
            return false;
        }

        // RS20: Matriz de autorización explícita.
        // Cada operación y su rol permitido se definen aquí de forma visible y auditable.
        return switch (operacion) {
            // --- Operaciones de Proyecto ---
            case VER_PROYECTO -> rol == Rol.OWNER || rol == Rol.COLLABORATOR || rol == Rol.VIEWER;
            case MODIFICAR_PROYECTO -> rol == Rol.OWNER;
            case ARCHIVAR_PROYECTO -> rol == Rol.OWNER;

            // --- Operaciones de Miembros ---
            case VER_MIEMBROS -> rol == Rol.OWNER || rol == Rol.COLLABORATOR || rol == Rol.VIEWER;
            case AGREGAR_MIEMBRO -> rol == Rol.OWNER;
            case ELIMINAR_MIEMBRO -> rol == Rol.OWNER;
            case MODIFICAR_ROLES -> rol == Rol.OWNER;

            // --- Operaciones de Archivos ---
            case VER_ARCHIVOS -> rol == Rol.OWNER || rol == Rol.COLLABORATOR || rol == Rol.VIEWER;
            case SUBIR_ARCHIVO -> rol == Rol.OWNER || rol == Rol.COLLABORATOR;
            case DESCARGAR_ARCHIVO -> rol == Rol.OWNER || rol == Rol.COLLABORATOR || rol == Rol.VIEWER;
            case ELIMINAR_ARCHIVO -> rol == Rol.OWNER || rol == Rol.COLLABORATOR;

            // --- Operaciones Administrativas (RS19) ---
            case VER_AUDITORIA -> rol == Rol.OWNER; // <-- AGREGADO PARA RS19

            // RS20 / RS6: Cualquier operación no listada explícitamente se deniega por defecto.
            default -> false;
        };
    }
}