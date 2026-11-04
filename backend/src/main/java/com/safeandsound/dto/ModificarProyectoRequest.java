package com.safeandsound.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;

public class ModificarProyectoRequest {
    private String nombre;
    private String descripcion;

    @NotNull
    private char[] passwordConfirmacion;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

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