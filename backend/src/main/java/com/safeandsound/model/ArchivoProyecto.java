package com.safeandsound.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "archivos_proyecto")
public class ArchivoProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String ruta;

    @Column(nullable = false)
    private Long tamano;

    @Column(nullable = false)
    private LocalDate fechaSubida;

    @Column(nullable = false)
    private boolean eliminado = false;

    @Column(name = "eliminado_en")
    private Instant eliminadoEn;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }

    public Long getTamano() { return tamano; }
    public void setTamano(Long tamano) { this.tamano = tamano; }

    public boolean isEliminado() { return eliminado; }

    public void setEliminado(boolean eliminado) { this.eliminado = eliminado;}

    public Instant getEliminadoEn() { return eliminadoEn; }

    public void setEliminadoEn(Instant eliminadoEn) { this.eliminadoEn = eliminadoEn; }

    public LocalDate getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDate fechaSubida) { this.fechaSubida = fechaSubida; }
}