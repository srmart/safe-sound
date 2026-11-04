
package com.safeandsound.exception;

import java.time.Instant;

public class LimiteIntentosExcedidoException extends RuntimeException {

    private final Instant bloqueadoHasta;

    public LimiteIntentosExcedidoException(Instant bloqueadoHasta) {
        super("Demasiados intentos. Intente nuevamente más tarde.");
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
