package com.safeandsound.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "solicitudes_restauracion")
public class SolicitudRestauracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Archivo concreto que se quiere restaurar
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "archivo_id", nullable = false)
    private Archivo archivo;

    // Proyecto al que pertenece el archivo
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    // Usuario que solicita la restauración
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    // Propietario que aprueba o rechaza la solicitud
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobador_id")
    private Usuario aprobador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudRestauracion estado =
            EstadoSolicitudRestauracion.PENDIENTE;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "aprobada_en")
    private Instant aprobadaEn;

    // Vencimiento de la autorización JIT
    @Column(name = "vence_en")
    private Instant venceEn;

    // Momento en que se utilizó la autorización
    @Column(name = "usada_en")
    private Instant usadaEn;

    public SolicitudRestauracion() {
        //in progress
    }

    @PrePersist
    protected void alCrear() {
        if (creadaEn == null) {
            creadaEn = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Archivo getArchivo() {
        return archivo;
    }

    public void setArchivo(Archivo archivo) {
        this.archivo = archivo;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
    }

    public Usuario getAprobador() {
        return aprobador;
    }

    public void setAprobador(Usuario aprobador) {
        this.aprobador = aprobador;
    }

    public EstadoSolicitudRestauracion getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitudRestauracion estado) {
        this.estado = estado;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getAprobadaEn() {
        return aprobadaEn;
    }

    public void setAprobadaEn(Instant aprobadaEn) {
        this.aprobadaEn = aprobadaEn;
    }

    public Instant getVenceEn() {
        return venceEn;
    }

    public void setVenceEn(Instant venceEn) {
        this.venceEn = venceEn;
    }

    public Instant getUsadaEn() {
        return usadaEn;
    }

    public void setUsadaEn(Instant usadaEn) {
        this.usadaEn = usadaEn;
    }
}
