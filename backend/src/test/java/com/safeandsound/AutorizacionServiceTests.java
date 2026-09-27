package com.safeandsound;

import com.safeandsound.model.Operacion;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.service.AutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutorizacionServiceTests {

    private ProyectoMiembroRepository proyectoMiembroRepository;
    private AutorizacionService autorizacionService;

    @BeforeEach
    void setUp() {
        proyectoMiembroRepository = mock(ProyectoMiembroRepository.class);
        autorizacionService = new AutorizacionService(proyectoMiembroRepository);
    }

    // =========================
    // RS6
    // =========================

    @Test
    void usuarioSinMembresiaDebeSerRechazado() {

        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L,
                        1L,
                        Operacion.VER_PROYECTO
                )
        );
    }

    @Test
    void viewerPuedeVerProyecto() {

        ProyectoMiembro miembro = mock(ProyectoMiembro.class);

        when(miembro.getRol()).thenReturn(Rol.VIEWER);

        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenReturn(Optional.of(miembro));

        assertDoesNotThrow(
                () -> autorizacionService.verificarPermiso(
                        2L,
                        1L,
                        Operacion.VER_PROYECTO
                )
        );
    }

    @Test
    void viewerNoPuedeModificarProyecto() {

        ProyectoMiembro miembro = mock(ProyectoMiembro.class);

        when(miembro.getRol()).thenReturn(Rol.VIEWER);

        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenReturn(Optional.of(miembro));

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L,
                        1L,
                        Operacion.MODIFICAR_PROYECTO
                )
        );
    }

    @Test
    void ausenciaDeRolDebeDenegarAcceso() {

        ProyectoMiembro miembro = mock(ProyectoMiembro.class);

        when(miembro.getRol()).thenReturn(null);

        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenReturn(Optional.of(miembro));

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L,
                        1L,
                        Operacion.VER_PROYECTO
                )
        );
    }

    @Test
    void ausenciaDeOperacionDebeDenegarAcceso() {

        ProyectoMiembro miembro = mock(ProyectoMiembro.class);

        when(miembro.getRol()).thenReturn(Rol.OWNER);

        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenReturn(Optional.of(miembro));

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L,
                        1L,
                        null
                )
        );
    }

    // =========================
    // RS9
    // =========================

    @Test
    void errorAlConsultarAutorizacionDebeDenegarAcceso() {

        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenThrow(
                        new RuntimeException(
                                "Error simulado de base de datos"
                        )
                );

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L,
                        1L,
                        Operacion.VER_PROYECTO
                )
        );
    }

    @Test
    void usuarioConPermisoTambienDebeSerRechazadoSiFallaLaAutorizacion() {

        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenThrow(
                        new RuntimeException(
                                "Error simulado al consultar permisos"
                        )
                );

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L,
                        1L,
                        Operacion.MODIFICAR_PROYECTO
                )
        );
    }
}


