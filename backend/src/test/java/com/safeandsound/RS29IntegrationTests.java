
package com.safeandsound;

import com.safeandsound.controller.ProyectoController;
import com.safeandsound.exception.GlobalExceptionHandler;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RS29IntegrationTests {

    private ProyectoService proyectoService;
    private AutorizacionService autorizacionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proyectoService = mock(ProyectoService.class);
        autorizacionService = mock(AutorizacionService.class);

        ProyectoController controller = new ProyectoController(
                proyectoService,
                autorizacionService
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken autenticar(Long usuarioId) {
        return new UsernamePasswordAuthenticationToken(
                usuarioId,
                null,
                Collections.emptyList()
        );
    }

    @Test
    void parametrosDelClienteNoDebenModificarIdentidad() throws Exception {
        Long usuarioAutenticado = 12L;
        Long proyectoId = 4L;

        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioAutenticado,
                        proyectoId,
                        Operacion.VER_PROYECTO
                );

        mockMvc.perform(
                        get("/proyectos/{proyectoId}", proyectoId)
                                .principal(autenticar(usuarioAutenticado))
                                .param("usuarioId", "11")
                                .param("rol", "OWNER")
                )
                .andExpect(status().isForbidden());

        verify(autorizacionService).verificarPermiso(
                usuarioAutenticado,
                proyectoId,
                Operacion.VER_PROYECTO
        );

        verify(proyectoService, never()).obtenerProyecto(anyLong());
    }

    @Test
    void identidadAutenticadaDebeLlegarAlServicioDeAutorizacion()
            throws Exception {

        Long usuarioAutenticado = 11L;
        Long proyectoId = 4L;

        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("BandaRock");

        when(proyectoService.obtenerProyecto(proyectoId))
                .thenReturn(proyecto);

        mockMvc.perform(
                        get("/proyectos/{proyectoId}", proyectoId)
                                .principal(autenticar(usuarioAutenticado))
                )
                .andExpect(status().isOk());

        verify(autorizacionService).verificarPermiso(
                usuarioAutenticado,
                proyectoId,
                Operacion.VER_PROYECTO
        );

        verify(proyectoService).obtenerProyecto(proyectoId);
    }

    @Test
    void usuarioSinPermisoNoDebeModificarProyecto() throws Exception {
        Long usuarioId = 12L;
        Long proyectoId = 4L;

        doThrow(new AccessDeniedException("Sin permisos"))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioId,
                        proyectoId,
                        Operacion.MODIFICAR_PROYECTO
                );

        mockMvc.perform(
                        put("/proyectos/{proyectoId}", proyectoId)
                                .principal(autenticar(usuarioId))
                                .param("nombre", "Proyecto modificado")
                )
                .andExpect(status().isForbidden());

        verify(proyectoService, never())
                .modificarProyecto(anyLong(), anyString(), any());
    }

    @Test
    void modificacionDebeAutorizarseSobreProyectoSolicitado()
            throws Exception {

        Long usuarioId = 11L;
        Long proyectoId = 4L;

        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("BandaRock");

        when(proyectoService.modificarProyecto(
                eq(proyectoId),
                eq("BandaRock"),
                isNull()
        )).thenReturn(proyecto);

        mockMvc.perform(
                        put("/proyectos/{proyectoId}", proyectoId)
                                .principal(autenticar(usuarioId))
                                .param("nombre", "BandaRock")
                )
                .andExpect(status().isOk());

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.MODIFICAR_PROYECTO
        );

        verify(proyectoService).modificarProyecto(
                proyectoId,
                "BandaRock",
                null
        );
    }
}
