package com.safeandsound.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "archivos")
public class Archivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subido_por", nullable = false)
    private Usuario subidoPor;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false, unique = true)
    private UUID nombreAlmacenado;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamanio", nullable = false)
    private Long tamanio;

    @Column(name = "fecha_subida", nullable = false)
    private OffsetDateTime fechaSubida;

    @Column(nullable = false)
    private boolean eliminado = false;

    @Column(name = "eliminado_en")
    private Instant eliminadoEn;

    @PrePersist
    protected void prePersist() {
        if (fechaSubida == null) {
            fechaSubida = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }

    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }

    public Usuario getSubidoPor() { return subidoPor; }
    public void setSubidoPor(Usuario subidoPor) { this.subidoPor = subidoPor; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public UUID getNombreAlmacenado() { return nombreAlmacenado; }
    public void setNombreAlmacenado(UUID nombreAlmacenado) { this.nombreAlmacenado = nombreAlmacenado; }

    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String tipoMime) { this.tipoMime = tipoMime; }

    public Long getTamanio() { return tamanio; }
    public void setTamanio(Long tamanio) { this.tamanio = tamanio; }

    public OffsetDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(OffsetDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public Instant getEliminadoEn() {
        return eliminadoEn;
    }

    public void setEliminadoEn(Instant eliminadoEn) {
        this.eliminadoEn = eliminadoEn;
    }
}