package com.safeandsound.dto;

import com.safeandsound.model.EstadoSolicitudRestauracion;
import com.safeandsound.model.SolicitudRestauracion;

import java.time.Instant;

public record SolicitudRestauracionResponse(
        Long id,
        Long archivoId,
        Long proyectoId,
        Long solicitanteId,
        String solicitanteUsername,
        EstadoSolicitudRestauracion estado,
        Instant creadaEn,
        Long aprobadorId,
        Instant aprobadaEn,
        Instant venceEn,
        Instant usadaEn
) {

    public static SolicitudRestauracionResponse basica(
            SolicitudRestauracion solicitud
    ) {
        return new SolicitudRestauracionResponse(
                solicitud.getId(),
                solicitud.getArchivo().getId(),
                solicitud.getProyecto().getId(),
                solicitud.getSolicitante().getId(),
                solicitud.getSolicitante().getUsername(),
                solicitud.getEstado(),
                solicitud.getCreadaEn(),
                null,
                null,
                null,
                null
        );
    }

    public static SolicitudRestauracionResponse administrativa(
            SolicitudRestauracion solicitud
    ) {
        return new SolicitudRestauracionResponse(
                solicitud.getId(),
                solicitud.getArchivo().getId(),
                solicitud.getProyecto().getId(),
                solicitud.getSolicitante().getId(),
                solicitud.getSolicitante().getUsername(),
                solicitud.getEstado(),
                solicitud.getCreadaEn(),
                solicitud.getAprobador() != null
                        ? solicitud.getAprobador().getId()
                        : null,
                solicitud.getAprobadaEn(),
                solicitud.getVenceEn(),
                solicitud.getUsadaEn()
        );
    }
}