package com.safeandsound;

import com.safeandsound.controller.ProyectoController;
import com.safeandsound.exception.GlobalExceptionHandler;
import com.safeandsound.model.Operacion;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class CrossTenantHttpTests {

    private ProyectoService proyectoService;
    private AutorizacionService autorizacionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        proyectoService = mock(ProyectoService.class);
        autorizacionService = mock(AutorizacionService.class);

        ProyectoController proyectoController =
                new ProyectoController(
                        proyectoService,
                        autorizacionService
                );

        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(proyectoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void accesoHttpAProyectoAjenoDebeDevolver403() throws Exception {

        Long usuarioId = 1L;
        Long proyectoAjenoId = 2L;

        doThrow(new AccessDeniedException(
                "El usuario no pertenece al proyecto"
        ))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioId,
                        proyectoAjenoId,
                        Operacion.VER_PROYECTO
                );

        var auth =
                new UsernamePasswordAuthenticationToken(
                        usuarioId,
                        null
                );

        mockMvc.perform(
                        get("/proyectos/{proyectoId}", proyectoAjenoId)
                                .principal(auth)
                )
                .andExpect(status().isForbidden())
                .andExpect(content().string(
                        "El usuario no pertenece al proyecto"
                ));

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoAjenoId,
                Operacion.VER_PROYECTO
        );

        verify(proyectoService, never())
                .obtenerProyecto(proyectoAjenoId);
    }
}