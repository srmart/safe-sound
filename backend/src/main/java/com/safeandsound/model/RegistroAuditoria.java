package com.safeandsound.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "registros_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(name = "operacion", nullable = false, length = 50)
    private String operacion;

    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @PrePersist
    protected void onCreate() {
        fechaHora = Instant.now();
    }

    public RegistroAuditoria() {}

    public RegistroAuditoria(Long usuarioId, Long proyectoId, String operacion, String detalles) {
        this.usuarioId = usuarioId;
        this.proyectoId = proyectoId;
        this.operacion = operacion;
        this.detalles = detalles;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getProyectoId() { return proyectoId; }
    public String getOperacion() { return operacion; }
    public String getDetalles() { return detalles; }
    public Instant getFechaHora() { return fechaHora; }
}