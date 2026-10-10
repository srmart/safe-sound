
package com.safeandsound;

import com.safeandsound.model.Proyecto;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.repository.ProyectoRepository;
import com.safeandsound.repository.UsuarioRepository;
import com.safeandsound.security.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RS29JwtDatabaseIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private ProyectoMiembroRepository proyectoMiembroRepository;

    @Test
    void usuarioAutenticadoNoPuedeAccederAProyectoAjeno()
            throws Exception {

        String identificador = UUID.randomUUID().toString().substring(0, 8);

        // Crear usuario propietario.
        Usuario propietario = new Usuario();
        propietario.setEmail("owner-" + identificador + "@test.com");
        propietario.setUsername("owner-" + identificador);
        propietario.setPasswordHash("hash-de-prueba");

        propietario = usuarioRepository.saveAndFlush(propietario);

        // Crear usuario externo.
        Usuario externo = new Usuario();
        externo.setEmail("externo-" + identificador + "@test.com");
        externo.setUsername("externo-" + identificador);
        externo.setPasswordHash("hash-de-prueba");

        externo = usuarioRepository.saveAndFlush(externo);

        // Crear proyecto perteneciente al propietario.
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto RS29");
        proyecto.setCreadoPor(propietario);

        proyecto = proyectoRepository.saveAndFlush(proyecto);

        // Asignar OWNER solamente al propietario.
        ProyectoMiembro miembro = new ProyectoMiembro();
        miembro.setProyecto(proyecto);
        miembro.setUsuario(propietario);
        miembro.setRol(Rol.OWNER);

        proyectoMiembroRepository.saveAndFlush(miembro);

        // Generar JWT válido para el usuario externo.
        String tokenExterno = jwtService.generateAccessToken(externo);

        // El usuario externo no pertenece al proyecto.
        // La API debe responder 403.
        mockMvc.perform(
                get("/proyectos/{proyectoId}", proyecto.getId())
                        .header("Authorization", "Bearer " + tokenExterno)
        ).andExpect(status().isForbidden());
    }
}
