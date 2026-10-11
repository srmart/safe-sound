package com.safeandsound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ActualizarPerfilRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 30, message = "El nombre de usuario debe tener entre 3 y 30 caracteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "El nombre de usuario solo puede contener letras, números y guiones bajos"
    )
    private String username;

    @Size(max = 1_500_000, message = "La foto de perfil es demasiado grande")
    private String fotoPerfil;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}
