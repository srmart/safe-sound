package com.safeandsound.repository;

import com.safeandsound.model.EstadoSolicitudRestauracion;
import com.safeandsound.model.SolicitudRestauracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudRestauracionRepository
        extends JpaRepository<SolicitudRestauracion, Long> {

    List<SolicitudRestauracion> findByProyectoId(Long proyectoId);

    List<SolicitudRestauracion> findByProyectoIdAndEstado(
            Long proyectoId,
            EstadoSolicitudRestauracion estado
    );

    Optional<SolicitudRestauracion> findByIdAndProyectoId(
            Long id,
            Long proyectoId
    );

    Optional<SolicitudRestauracion>
    findByIdAndSolicitanteIdAndEstado(
            Long id,
            Long solicitanteId,
            EstadoSolicitudRestauracion estado
    );
}