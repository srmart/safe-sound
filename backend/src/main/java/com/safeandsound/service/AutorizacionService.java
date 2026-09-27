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

        ProyectoMiembro miembro = proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(proyectoId, usuarioId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "El usuario no pertenece al proyecto"
                        )
                );

        Rol rol = miembro.getRol();

        if (!tienePermiso(rol, operacion)) {
            throw new AccessDeniedException(
                    "El rol " + rol + " no tiene permiso para " + operacion
            );
        }
    }

    private boolean tienePermiso(Rol rol, Operacion operacion) {

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
        };
    }
}