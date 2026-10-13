package com.safeandsound.dto;

import java.time.Instant;

public record AuditoriaResponse(
        Long id,
        Long usuarioId,
        Long proyectoId,
        String operacion,
        String detalles,
        Instant fechaHora
) {}