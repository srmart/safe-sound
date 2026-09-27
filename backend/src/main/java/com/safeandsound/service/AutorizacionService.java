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

        return switch (operacion) {

            case VER_PROYECTO ->
                    rol == Rol.OWNER ||
                            rol == Rol.COLLABORATOR ||
                            rol == Rol.VIEWER;

            case MODIFICAR_PROYECTO ->
                    rol == Rol.OWNER;

            case ARCHIVAR_PROYECTO ->
                    rol == Rol.OWNER;

            case VER_MIEMBROS ->
                    rol == Rol.OWNER ||
                            rol == Rol.COLLABORATOR ||
                            rol == Rol.VIEWER;

            case MODIFICAR_ROLES ->
                    rol == Rol.OWNER;

            default -> false;
        };
    }
}