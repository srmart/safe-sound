
package com.safeandsound;

import com.safeandsound.model.Operacion;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.service.AutorizacionService;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RBACVerificationTests {

    private static Stream<Arguments> permisosPorRol() {
        return Stream.of(
                // OWNER
                Arguments.of(Rol.OWNER, Operacion.VER_PROYECTO, true),
                Arguments.of(Rol.OWNER, Operacion.MODIFICAR_PROYECTO, true),
                Arguments.of(Rol.OWNER, Operacion.ARCHIVAR_PROYECTO, true),
                Arguments.of(Rol.OWNER, Operacion.VER_MIEMBROS, true),
                Arguments.of(Rol.OWNER, Operacion.MODIFICAR_ROLES, true),

                // COLLABORATOR
                Arguments.of(Rol.COLLABORATOR, Operacion.VER_PROYECTO, true),
                Arguments.of(Rol.COLLABORATOR, Operacion.MODIFICAR_PROYECTO, false),
                Arguments.of(Rol.COLLABORATOR, Operacion.ARCHIVAR_PROYECTO, false),
                Arguments.of(Rol.COLLABORATOR, Operacion.VER_MIEMBROS, true),
                Arguments.of(Rol.COLLABORATOR, Operacion.MODIFICAR_ROLES, false),

                // VIEWER
                Arguments.of(Rol.VIEWER, Operacion.VER_PROYECTO, true),
                Arguments.of(Rol.VIEWER, Operacion.MODIFICAR_PROYECTO, false),
                Arguments.of(Rol.VIEWER, Operacion.ARCHIVAR_PROYECTO, false),
                Arguments.of(Rol.VIEWER, Operacion.VER_MIEMBROS, true),
                Arguments.of(Rol.VIEWER, Operacion.MODIFICAR_ROLES, false)
        );
    }

    @ParameterizedTest(name = "{index}: {0} - {1} - permitido={2}")
    @MethodSource("permisosPorRol")
    void verificarMatrizDePermisos(
            Rol rol,
            Operacion operacion,
            boolean permitido
    ) {
        Long usuarioId = 1L;
        Long proyectoId = 10L;

        ProyectoMiembroRepository repository =
                mock(ProyectoMiembroRepository.class);

        AutorizacionService autorizacionService =
                new AutorizacionService(repository);

        ProyectoMiembro miembro = mock(ProyectoMiembro.class);

        when(miembro.getRol()).thenReturn(rol);

        when(repository.findByProyectoIdAndUsuarioId(
                proyectoId, usuarioId
        )).thenReturn(Optional.of(miembro));

        if (permitido) {
            assertDoesNotThrow(() ->
                    autorizacionService.verificarPermiso(
                            usuarioId, proyectoId, operacion
                    )
            );
        } else {
            assertThrows(AccessDeniedException.class, () ->
                    autorizacionService.verificarPermiso(
                            usuarioId, proyectoId, operacion
                    )
            );
        }

        verify(repository).findByProyectoIdAndUsuarioId(
                proyectoId, usuarioId
        );
    }
}
