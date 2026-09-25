package com.safeandsound.exception;

public class UsernameYaRegistradoException extends RuntimeException {

    public UsernameYaRegistradoException() {
        super("El nombre de usuario ya está registrado");
    }
}