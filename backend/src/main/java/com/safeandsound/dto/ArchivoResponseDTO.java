package com.safeandsound.dto;

import com.safeandsound.model.Archivo;
import java.time.OffsetDateTime;

public record ArchivoResponseDTO(
        Long id,
        String nombre,
        Long tamano,
        OffsetDateTime fechaSubida,
        UsuarioResumenDTO usuario
) {

    public record UsuarioResumenDTO(
            Long id,
            String username,
            String email
    ) {}

    public static ArchivoResponseDTO desdeEntidad(Archivo archivo) {
        return new ArchivoResponseDTO(
                archivo.getId(),
                archivo.getNombreOriginal(),
                archivo.getTamanio(),
                archivo.getFechaSubida(),
                new UsuarioResumenDTO(
                        archivo.getSubidoPor().getId(),
                        archivo.getSubidoPor().getUsername(),
                        archivo.getSubidoPor().getEmail()
                )
        );
    }
}