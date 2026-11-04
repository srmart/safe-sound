
package com.safeandsound.repository;

import com.safeandsound.model.ControlIntentosAutenticacion;
import com.safeandsound.model.ControlIntentosAutenticacion.TipoIntento;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ControlIntentosAutenticacionRepository
        extends JpaRepository<ControlIntentosAutenticacion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c FROM ControlIntentosAutenticacion c
        WHERE c.identificador = :identificador
        AND c.tipo = :tipo
    """)
    Optional<ControlIntentosAutenticacion> buscarParaActualizar(
            @Param("identificador") String identificador,
            @Param("tipo") TipoIntento tipo
    );
}
