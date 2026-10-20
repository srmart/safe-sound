
package com.safeandsound.dto;

import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;

public record ProyectoMiembroResponse(
        Long usuarioId,
        String username,
        Rol rol
) {
    public static ProyectoMiembroResponse desde(
            ProyectoMiembro miembro
    ) {
        return new ProyectoMiembroResponse(
                miembro.getUsuario().getId(),
                miembro.getUsuario().getUsername(),
                miembro.getRol()
        );
    }
}
