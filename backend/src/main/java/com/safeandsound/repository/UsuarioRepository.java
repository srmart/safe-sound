package com.safeandsound.repository;

import com.safeandsound.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


// spring Data genera automáticamente la consulta para comprobar si el email existe.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    //busca un usuario por email para autenticarlo
    Optional<Usuario> findByEmail(String email);
}