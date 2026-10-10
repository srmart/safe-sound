package com.safeandsound.dto;

import com.safeandsound.model.Rol;

public record ProyectoListadoDTO(
        Long id,
        String nombre,
        String descripcion,
        boolean activo,
        Rol rol
) {
}
