package com.safeandsound.dto;

import com.safeandsound.model.Rol;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;

public class ModificarMiembroRequest {
    @NotNull
    private Rol rol;

    @NotNull
    private char[] passwordConfirmacion;

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

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