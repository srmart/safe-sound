package com.safeandsound.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "intentos_acceso_protegido")
public class IntentoAccesoProtegido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 255)
    private String recurso;

    @Column(nullable = false, length = 16)
    private String operacion;

    @Column(nullable = false)
    private boolean autorizado;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    @Column(name = "codigo_respuesta", nullable = false)
    private int codigoRespuesta;

    protected IntentoAccesoProtegido() {
    }

    public IntentoAccesoProtegido(
            Long usuarioId,
            String recurso,
            String operacion,
            boolean autorizado,
            int codigoRespuesta
    ) {
        this.usuarioId = usuarioId;
        this.recurso = recurso;
        this.operacion = operacion;
        this.autorizado = autorizado;
        this.codigoRespuesta = codigoRespuesta;
        this.fechaHora = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getOperacion() {
        return operacion;
    }

    public boolean isAutorizado() {
        return autorizado;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public int getCodigoRespuesta() {
        return codigoRespuesta;
    }
}
