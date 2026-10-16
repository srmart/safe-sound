package com.safeandsound.dto;

import com.safeandsound.model.Proyecto;

public record ProyectoResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activo,
        Long creadoPorId,
        String creadoPorUsername
) {
    public static ProyectoResponse desde(Proyecto proyecto) {
        return new ProyectoResponse(
                proyecto.getId(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.isActivo(),
                proyecto.getCreadoPor().getId(),
                proyecto.getCreadoPor().getUsername()
        );
    }
}