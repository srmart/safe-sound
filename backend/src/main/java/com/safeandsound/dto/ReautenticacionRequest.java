package com.safeandsound.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;

public class ReautenticacionRequest {

    @NotNull(message = "La contraseña es obligatoria")
    private char[] password;

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public void clearPassword() {
        if (this.password != null) {
            Arrays.fill(this.password, '\0');
        }
    }
}
