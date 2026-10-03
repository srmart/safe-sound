package com.safeandsound.repository;

import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ProyectoMiembroRepository extends JpaRepository<ProyectoMiembro, Long> {

    List<ProyectoMiembro> findByUsuarioId(Long usuarioId);

    Optional<ProyectoMiembro> findByProyectoIdAndUsuarioId(
            Long proyectoId,
            Long usuarioId
    );

    boolean existsByProyectoIdAndUsuarioId(
            Long proyectoId,
            Long usuarioId
    );

    boolean existsByProyectoIdAndUsuarioIdAndRol(
            Long proyectoId,
            Long usuarioId,
            Rol rol
    );
}