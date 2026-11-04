
package com.safeandsound.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "control_intentos_autenticacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_control_intentos_identificador_tipo",
                columnNames = {"identificador", "tipo"}
        )
)
public class ControlIntentosAutenticacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String identificador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoIntento tipo;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "ultimo_intento", nullable = false)
    private Instant ultimoIntento;

    protected ControlIntentosAutenticacion() {}

    public Long getId() { return id; }
    public String getIdentificador() { return identificador; }
    public TipoIntento getTipo() { return tipo; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public Instant getUltimoIntento() { return ultimoIntento; }

    public void registrarFallo(Instant ahora) {
        intentosFallidos++;
        ultimoIntento = ahora;

        if (intentosFallidos >= 5) {
            bloqueadoHasta = ahora.plusSeconds(900);
        }
    }

    public void reiniciar(Instant ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoIntento = ahora;
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null &&
                bloqueadoHasta.isAfter(ahora);
    }

    public enum TipoIntento {
        LOGIN,
        REAUTENTICACION
    }
}
