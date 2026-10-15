
package com.safeandsound.service;

import com.safeandsound.model.ArchivoProyecto;
import com.safeandsound.model.EstadoSolicitudRestauracion;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.SolicitudRestauracion;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.ArchivoProyectoRepository;
import com.safeandsound.repository.ProyectoRepository;
import com.safeandsound.repository.SolicitudRestauracionRepository;
import com.safeandsound.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class SolicitudRestauracionService {

    private final SolicitudRestauracionRepository solicitudRepository;
    private final ArchivoProyectoRepository archivoRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacionService;

    private final long tiempoGraciaHoras;
    private final long duracionJitMinutos;

    public SolicitudRestauracionService(
            SolicitudRestauracionRepository solicitudRepository,
            ArchivoProyectoRepository archivoRepository,
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository,
            AutorizacionService autorizacionService,
            @Value("${app.archivos.tiempo-gracia-horas:3}")
            long tiempoGraciaHoras,
            @Value("${app.jit.duracion-minutos:10}")
            long duracionJitMinutos) {

        this.solicitudRepository = solicitudRepository;
        this.archivoRepository = archivoRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.autorizacionService = autorizacionService;
        this.tiempoGraciaHoras = tiempoGraciaHoras;
        this.duracionJitMinutos = duracionJitMinutos;
    }

    @Transactional
    public SolicitudRestauracion solicitarRestauracion(
            Long usuarioId,
            Long proyectoId,
            Long archivoId) {

        autorizacionService.verificarPermiso(
                usuarioId, proyectoId,
                Operacion.SOLICITAR_RESTAURACION);

        ArchivoProyecto archivo = archivoRepository
                .findByIdAndProyectoIdAndEliminadoTrue(
                        archivoId, proyectoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un archivo eliminado con ese ID en el proyecto"));

        validarPeriodoDeGracia(archivo);

        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        Usuario solicitante = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        SolicitudRestauracion solicitud = new SolicitudRestauracion();
        solicitud.setArchivo(archivo);
        solicitud.setProyecto(proyecto);
        solicitud.setSolicitante(solicitante);
        solicitud.setEstado(EstadoSolicitudRestauracion.PENDIENTE);

        return solicitudRepository.save(solicitud);
    }

    @Transactional
    public SolicitudRestauracion aprobarSolicitud(
            Long ownerId,
            Long proyectoId,
            Long solicitudId) {

        autorizacionService.verificarPermiso(
                ownerId, proyectoId,
                Operacion.APROBAR_SOLICITUD_RESTAURACION);

        SolicitudRestauracion solicitud = obtenerSolicitud(
                solicitudId, proyectoId);

        if (solicitud.getEstado() != EstadoSolicitudRestauracion.PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La solicitud ya fue procesada");
        }

        if (solicitud.getSolicitante().getId().equals(ownerId)) {
            throw new AccessDeniedException(
                    "No se puede aprobar una solicitud propia");
        }

        validarPeriodoDeGracia(solicitud.getArchivo());

        Instant ahora = Instant.now();

        solicitud.setAprobador(usuarioRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado")));

        solicitud.setAprobadaEn(ahora);
        solicitud.setVenceEn(ahora.plus(duracionJitMinutos, ChronoUnit.MINUTES));
        solicitud.setEstado(EstadoSolicitudRestauracion.APROBADA);

        return solicitudRepository.save(solicitud);
    }

    @Transactional
    public SolicitudRestauracion rechazarSolicitud(
            Long ownerId,
            Long proyectoId,
            Long solicitudId) {

        autorizacionService.verificarPermiso(
                ownerId, proyectoId,
                Operacion.RECHAZAR_SOLICITUD_RESTAURACION);

        SolicitudRestauracion solicitud = obtenerSolicitud(
                solicitudId, proyectoId);

        if (solicitud.getEstado() != EstadoSolicitudRestauracion.PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La solicitud ya fue procesada");
        }

        if (solicitud.getSolicitante().getId().equals(ownerId)) {
            throw new AccessDeniedException(
                    "No se puede procesar una solicitud propia");
        }

        solicitud.setAprobador(usuarioRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado")));

        solicitud.setEstado(EstadoSolicitudRestauracion.RECHAZADA);

        return solicitudRepository.save(solicitud);
    }

    @Transactional
    public ArchivoProyecto restaurarArchivo(
            Long usuarioId,
            Long proyectoId,
            Long solicitudId) {

        // No se utiliza RESTAURAR_ARCHIVO en la matriz permanente:
        // la autorización se comprueba contra la solicitud JIT concreta.
        SolicitudRestauracion solicitud = solicitudRepository
                .findByIdAndSolicitanteIdAndEstado(
                        solicitudId,
                        usuarioId,
                        EstadoSolicitudRestauracion.APROBADA)
                .orElseThrow(() -> new AccessDeniedException(
                        "No existe una autorización JIT vigente para este usuario"));

        if (!solicitud.getProyecto().getId().equals(proyectoId)) {
            throw new AccessDeniedException(
                    "La solicitud no pertenece a este proyecto");
        }

        Instant ahora = Instant.now();

        if (solicitud.getUsadaEn() != null
                || solicitud.getVenceEn() == null
                || !ahora.isBefore(solicitud.getVenceEn())) {

            solicitud.setEstado(EstadoSolicitudRestauracion.VENCIDA);
            solicitudRepository.save(solicitud);

            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "La autorización JIT venció o ya fue utilizada");
        }

        ArchivoProyecto archivo = solicitud.getArchivo();

        validarPeriodoDeGracia(archivo);

        if (!archivo.isEliminado()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El archivo ya fue restaurado");
        }

        Path ruta = Paths.get(archivo.getRuta()).toAbsolutePath().normalize();

        if (!Files.isRegularFile(ruta)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "El archivo físico ya no está disponible");
        }

        archivo.setEliminado(false);
        archivo.setEliminadoEn(null);

        solicitud.setUsadaEn(ahora);
        solicitud.setEstado(EstadoSolicitudRestauracion.UTILIZADA);

        archivoRepository.save(archivo);
        solicitudRepository.save(solicitud);

        return archivo;
    }

    @Transactional(readOnly = true)
    public java.util.List<SolicitudRestauracion> listarSolicitudes(
            Long proyectoId) {

        return solicitudRepository.findByProyectoId(proyectoId);
    }


    private SolicitudRestauracion obtenerSolicitud(
            Long solicitudId,
            Long proyectoId) {

        return solicitudRepository.findByIdAndProyectoId(
                        solicitudId, proyectoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada en el proyecto"));
    }

    private void validarPeriodoDeGracia(ArchivoProyecto archivo) {
        Instant eliminadoEn = archivo.getEliminadoEn();

        if (!archivo.isEliminado() || eliminadoEn == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El archivo no está disponible para restauración");
        }

        Instant limite = eliminadoEn.plus(
                tiempoGraciaHoras, ChronoUnit.HOURS);

        if (!Instant.now().isBefore(limite)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "El periodo de gracia para restaurar el archivo venció");
        }
    }
}
