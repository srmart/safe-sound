package com.safeandsound.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Arrays;

public class ReautenticacionRequest {
    
    @NotBlank(message = "La contraseña es obligatoria")
    private char[] password;

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    // Método para limpiar la contraseña de la memoria después de usarla
    public void clearPassword() {
        if (this.password != null) {
            Arrays.fill(this.password, '\0');
        }
    }
}