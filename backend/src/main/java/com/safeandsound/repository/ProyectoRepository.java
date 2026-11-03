package com.safeandsound.repository;

import com.safeandsound.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Proyecto p WHERE p.id = :proyectoId")
    Optional<Proyecto> buscarPorIdConBloqueo(
            @Param("proyectoId") Long proyectoId
    );

}