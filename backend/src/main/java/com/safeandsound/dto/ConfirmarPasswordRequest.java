package com.safeandsound.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;

public class ConfirmarPasswordRequest {
    @NotNull
    private char[] passwordConfirmacion;

    public char[] getPasswordConfirmacion() { return passwordConfirmacion; }
    public void setPasswordConfirmacion(char[] passwordConfirmacion) {
        this.passwordConfirmacion = passwordConfirmacion;
    }

    public void clearPassword() {
        if (passwordConfirmacion != null) {
            Arrays.fill(passwordConfirmacion, '\0');
        }
    }
}