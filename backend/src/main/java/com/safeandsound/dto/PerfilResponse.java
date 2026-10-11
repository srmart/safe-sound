package com.safeandsound.dto;

import com.safeandsound.model.Usuario;

public record PerfilResponse(
        Long id,
        String email,
        String username,
        String fotoPerfil,
        boolean notificacionesHabilitadas,
        boolean modoOscuro
) {
    public static PerfilResponse from(Usuario usuario) {
        return new PerfilResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getUsername(),
                usuario.getFotoPerfil(),
                usuario.isNotificacionesHabilitadas(),
                usuario.isModoOscuro()
        );
    }
}
