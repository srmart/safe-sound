package com.safeandsound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Column(name = "foto_perfil", columnDefinition = "TEXT")
    private String fotoPerfil;

    @Column(name = "notificaciones_habilitadas", nullable = false)
    private boolean notificacionesHabilitadas = true;

    @Column(name = "modo_oscuro", nullable = false)
    private boolean modoOscuro = false;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public boolean isNotificacionesHabilitadas() {
        return notificacionesHabilitadas;
    }

    public void setNotificacionesHabilitadas(boolean notificacionesHabilitadas) {
        this.notificacionesHabilitadas = notificacionesHabilitadas;
    }

    public boolean isModoOscuro() {
        return modoOscuro;
    }

    public void setModoOscuro(boolean modoOscuro) {
        this.modoOscuro = modoOscuro;
    }
}
