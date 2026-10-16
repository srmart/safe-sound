
package com.safeandsound.controller;

import com.safeandsound.model.EstadoSolicitudRestauracion;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.SolicitudRestauracion;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.SolicitudRestauracionService;
import com.safeandsound.dto.SolicitudRestauracionResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/proyectos/{proyectoId}/solicitudes-restauracion")
public class SolicitudRestauracionController {

    private final SolicitudRestauracionService solicitudService;
    private final AutorizacionService autorizacionService;

    public SolicitudRestauracionController(
            SolicitudRestauracionService solicitudService,
            AutorizacionService autorizacionService
    ) {
        this.solicitudService = solicitudService;
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    public ResponseEntity<List<SolicitudRestauracionResponse>> listarSolicitudes(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(
                usuarioId, proyectoId, Operacion.APROBAR_SOLICITUD_RESTAURACION
        );

        List<SolicitudRestauracionResponse> solicitudes = solicitudService
                .listarSolicitudes(proyectoId)
                .stream()
                .map(SolicitudRestauracionResponse::administrativa)
                .toList();

        return ResponseEntity.ok(solicitudes);
    }

    @PostMapping("/{solicitudId}/aprobar")
    public ResponseEntity<SolicitudRestauracionResponse> aprobarSolicitud(
            @PathVariable Long proyectoId,
            @PathVariable Long solicitudId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        SolicitudRestauracion solicitud = solicitudService.aprobarSolicitud(
                usuarioId, proyectoId, solicitudId
        );

        return ResponseEntity.ok(
                SolicitudRestauracionResponse.administrativa(solicitud)
        );
    }

    @PostMapping("/{solicitudId}/rechazar")
    public ResponseEntity<SolicitudRestauracionResponse> rechazarSolicitud(
            @PathVariable Long proyectoId,
            @PathVariable Long solicitudId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        SolicitudRestauracion solicitud = solicitudService.rechazarSolicitud(
                usuarioId, proyectoId, solicitudId
        );

        return ResponseEntity.ok(
                SolicitudRestauracionResponse.administrativa(solicitud)
        );
    }

    @PostMapping("/{solicitudId}/restaurar")
    public ResponseEntity<Void> restaurarArchivo(
            @PathVariable Long proyectoId,
            @PathVariable Long solicitudId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        solicitudService.restaurarArchivo(
                usuarioId, proyectoId, solicitudId
        );

        return ResponseEntity.noContent().build();
    }
}
