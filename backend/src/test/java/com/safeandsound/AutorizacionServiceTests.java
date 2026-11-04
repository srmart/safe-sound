
package com.safeandsound;

import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.repository.ProyectoRepository;
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
    private ProyectoRepository proyectoRepository;
    private AutorizacionService autorizacionService;

    @BeforeEach
    void setUp() {
        proyectoMiembroRepository = mock(ProyectoMiembroRepository.class);
        proyectoRepository = mock(ProyectoRepository.class);

        autorizacionService = new AutorizacionService(
                proyectoMiembroRepository,
                proyectoRepository
        );
    }

    // =========================
    // RS6 - Denegación por defecto
    // =========================

    @Test
    void usuarioSinMembresiaDebeSerRechazado() {
        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, Operacion.VER_PROYECTO
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
                        2L, 1L, Operacion.VER_PROYECTO
                )
        );
    }

    @Test
    void ausenciaDeOperacionDebeDenegarAcceso() {
        configurarMembresia(Rol.OWNER);

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, null
                )
        );
    }

    @Test
    void viewerPuedeVerProyecto() {
        configurarMembresia(Rol.VIEWER);

        assertDoesNotThrow(
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, Operacion.VER_PROYECTO
                )
        );
    }

    @Test
    void viewerNoPuedeModificarProyecto() {
        configurarMembresia(Rol.VIEWER);

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, Operacion.MODIFICAR_PROYECTO
                )
        );
    }

    // =========================
    // RS9 - Fallo seguro
    // =========================

    @Test
    void errorAlConsultarAutorizacionDebeDenegarAcceso() {
        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenThrow(new RuntimeException(
                        "Error simulado de base de datos"
                ));

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, Operacion.VER_PROYECTO
                )
        );
    }

    @Test
    void falloDeConsultaDebeDenegarInclusoUnaOperacionDeOwner() {
        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenThrow(new RuntimeException(
                        "Error simulado al consultar permisos"
                ));

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, Operacion.MODIFICAR_PROYECTO
                )
        );
    }

    // =========================
    // RS31 - Aislamiento entre proyectos
    // =========================

    @Test
    void ownerDeUnProyectoNoTienePermisosSobreOtroProyecto() {
        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(2L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        1L, 2L, Operacion.MODIFICAR_PROYECTO
                )
        );
    }

    @Test
    void collaboratorDeUnProyectoNoTienePermisosSobreOtroProyecto() {
        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(2L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L, 2L, Operacion.VER_PROYECTO
                )
        );
    }

    @Test
    void viewerDeUnProyectoNoTienePermisosSobreOtroProyecto() {
        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(2L, 3L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        3L, 2L, Operacion.VER_PROYECTO
                )
        );
    }

    // =========================
    // INC-9 / RS13 - Estado del proyecto
    // =========================

    @Test
    void proyectoArchivadoDebeRechazarModificaciones() {
        configurarMembresia(Rol.OWNER);
        configurarProyecto(false);

        assertThrows(
                AccessDeniedException.class,
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, Operacion.MODIFICAR_ROLES
                )
        );
    }

    @Test
    void proyectoActivoDebePermitirModificacionesAutorizadas() {
        configurarMembresia(Rol.OWNER);
        configurarProyecto(true);

        assertDoesNotThrow(
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, Operacion.MODIFICAR_ROLES
                )
        );
    }

    @Test
    void proyectoArchivadoDebePermitirLecturasAutorizadas() {
        configurarMembresia(Rol.VIEWER);
        configurarProyecto(false);

        assertDoesNotThrow(
                () -> autorizacionService.verificarPermiso(
                        2L, 1L, Operacion.VER_PROYECTO
                )
        );
    }

    private void configurarMembresia(Rol rol) {
        ProyectoMiembro miembro = mock(ProyectoMiembro.class);

        when(miembro.getRol()).thenReturn(rol);

        when(proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(1L, 2L))
                .thenReturn(Optional.of(miembro));
    }

    private void configurarProyecto(boolean activo) {
        Proyecto proyecto = mock(Proyecto.class);

        when(proyecto.isActivo()).thenReturn(activo);
        when(proyectoRepository.findById(1L))
                .thenReturn(Optional.of(proyecto));
    }
}
