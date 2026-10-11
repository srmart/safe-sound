
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RBACHttpIntegrationTests {

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

    private Usuario crearUsuario(String prefijo) {
        String id = UUID.randomUUID().toString().substring(0, 8);

        Usuario usuario = new Usuario();
        usuario.setUsername(prefijo + "-" + id);
        usuario.setEmail(prefijo + "-" + id + "@test.com");
        usuario.setPasswordHash("hash-de-prueba");

        return usuarioRepository.saveAndFlush(usuario);
    }

    private Proyecto crearProyecto(Usuario propietario) {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto RBAC");
        proyecto.setDescripcion("Prueba de autorizacion");
        proyecto.setCreadoPor(propietario);
        proyecto.setActivo(true);

        return proyectoRepository.saveAndFlush(proyecto);
    }

    private void asignarRol(Proyecto proyecto, Usuario usuario, Rol rol) {
        ProyectoMiembro miembro = new ProyectoMiembro();
        miembro.setProyecto(proyecto);
        miembro.setUsuario(usuario);
        miembro.setRol(rol);

        proyectoMiembroRepository.saveAndFlush(miembro);
    }

    @Test
    void ownerPuedeConsultarYModificarProyecto() throws Exception {
        Usuario owner = crearUsuario("owner");
        Proyecto proyecto = crearProyecto(owner);
        asignarRol(proyecto, owner, Rol.OWNER);

        String token = jwtService.generateAccessToken(owner);

        mockMvc.perform(get("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("nombre", "Proyecto actualizado"))
                .andExpect(status().isOk());
    }

    @Test
    void collaboratorPuedeConsultarPeroNoModificar() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario collaborator = crearUsuario("collab");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, collaborator, Rol.COLLABORATOR);

        String token = jwtService.generateAccessToken(collaborator);

        mockMvc.perform(get("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("nombre", "Cambio no autorizado"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerPuedeConsultarPeroNoModificar() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario viewer = crearUsuario("viewer");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, viewer, Rol.VIEWER);

        String token = jwtService.generateAccessToken(viewer);

        mockMvc.perform(get("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("nombre", "Cambio no autorizado"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioExternoNoPuedeConsultarNiModificar() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario externo = crearUsuario("externo");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);

        String token = jwtService.generateAccessToken(externo);

        mockMvc.perform(get("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("nombre", "Cambio no autorizado"))
                .andExpect(status().isForbidden());
    }

    @Test
    void parametrosDelClienteNoOtorganPermisos() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario viewer = crearUsuario("viewer");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, viewer, Rol.VIEWER);

        String token = jwtService.generateAccessToken(viewer);

        mockMvc.perform(put("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("nombre", "Cambio no autorizado")
                        .param("usuarioId", owner.getId().toString())
                        .param("rol", "OWNER"))
                .andExpect(status().isForbidden());
    }


    @Test
    void ownerPuedeArchivarProyecto() throws Exception {
        Usuario owner = crearUsuario("owner");
        Proyecto proyecto = crearProyecto(owner);
        asignarRol(proyecto, owner, Rol.OWNER);

        String token = jwtService.generateAccessToken(owner);

        mockMvc.perform(delete("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertFalse(
                proyectoRepository.findById(proyecto.getId())
                        .orElseThrow()
                        .isActivo()
        );
    }

    @Test
    void collaboratorNoPuedeArchivarProyecto() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario collaborator = crearUsuario("collab");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, collaborator, Rol.COLLABORATOR);

        String token = jwtService.generateAccessToken(collaborator);

        mockMvc.perform(delete("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertTrue(
                proyectoRepository.findById(proyecto.getId())
                        .orElseThrow()
                        .isActivo()
        );
    }

    @Test
    void viewerNoPuedeArchivarProyecto() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario viewer = crearUsuario("viewer");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, viewer, Rol.VIEWER);

        String token = jwtService.generateAccessToken(viewer);

        mockMvc.perform(delete("/proyectos/{id}", proyecto.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertTrue(
                proyectoRepository.findById(proyecto.getId())
                        .orElseThrow()
                        .isActivo()
        );
    }


    @Test
    void ownerPuedeAgregarMiembro() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario nuevo = crearUsuario("nuevo");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);

        String token = jwtService.generateAccessToken(owner);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/miembros/{usuarioId}",
                        proyecto.getId(), nuevo.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .param("rol", "VIEWER"))
                .andExpect(status().isCreated());

        assertTrue(
                proyectoMiembroRepository.existsByProyectoIdAndUsuarioIdAndRol(
                        proyecto.getId(), nuevo.getId(), Rol.VIEWER
                )
        );
    }

    @Test
    void collaboratorNoPuedeAgregarMiembro() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario collaborator = crearUsuario("collab");
        Usuario nuevo = crearUsuario("nuevo");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, collaborator, Rol.COLLABORATOR);

        String token = jwtService.generateAccessToken(collaborator);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/miembros/{usuarioId}",
                        proyecto.getId(), nuevo.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .param("rol", "VIEWER"))
                .andExpect(status().isForbidden());

        assertFalse(
                proyectoMiembroRepository.existsByProyectoIdAndUsuarioId(
                        proyecto.getId(), nuevo.getId()
                )
        );
    }

    @Test
    void viewerNoPuedeAgregarMiembro() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario viewer = crearUsuario("viewer");
        Usuario nuevo = crearUsuario("nuevo");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, viewer, Rol.VIEWER);

        String token = jwtService.generateAccessToken(viewer);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/miembros/{usuarioId}",
                        proyecto.getId(), nuevo.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .param("rol", "VIEWER"))
                .andExpect(status().isForbidden());

        assertFalse(
                proyectoMiembroRepository.existsByProyectoIdAndUsuarioId(
                        proyecto.getId(), nuevo.getId()
                )
        );
    }


    @Test
    void ownerPuedeModificarRolDeMiembro() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario miembro = crearUsuario("miembro");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, miembro, Rol.VIEWER);

        String token = jwtService.generateAccessToken(owner);

        mockMvc.perform(put(
                        "/proyectos/{proyectoId}/miembros/{usuarioId}/rol",
                        proyecto.getId(), miembro.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .param("rol", "COLLABORATOR"))
                .andExpect(status().isNoContent());

        assertTrue(
                proyectoMiembroRepository.existsByProyectoIdAndUsuarioIdAndRol(
                        proyecto.getId(), miembro.getId(), Rol.COLLABORATOR
                )
        );
    }

    @Test
    void collaboratorNoPuedeModificarRolDeMiembro() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario collaborator = crearUsuario("collab");
        Usuario miembro = crearUsuario("miembro");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, collaborator, Rol.COLLABORATOR);
        asignarRol(proyecto, miembro, Rol.VIEWER);

        String token = jwtService.generateAccessToken(collaborator);

        mockMvc.perform(put(
                        "/proyectos/{proyectoId}/miembros/{usuarioId}/rol",
                        proyecto.getId(), miembro.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .param("rol", "OWNER"))
                .andExpect(status().isForbidden());

        assertTrue(
                proyectoMiembroRepository.existsByProyectoIdAndUsuarioIdAndRol(
                        proyecto.getId(), miembro.getId(), Rol.VIEWER
                )
        );
    }

    @Test
    void viewerNoPuedeModificarRolDeMiembro() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario viewer = crearUsuario("viewer");
        Usuario miembro = crearUsuario("miembro");
        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, viewer, Rol.VIEWER);
        asignarRol(proyecto, miembro, Rol.COLLABORATOR);

        String token = jwtService.generateAccessToken(viewer);

        mockMvc.perform(put(
                        "/proyectos/{proyectoId}/miembros/{usuarioId}/rol",
                        proyecto.getId(), miembro.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .param("rol", "OWNER"))
                .andExpect(status().isForbidden());

        assertTrue(
                proyectoMiembroRepository.existsByProyectoIdAndUsuarioIdAndRol(
                        proyecto.getId(), miembro.getId(), Rol.COLLABORATOR
                )
        );
    }


    @Test
    void listadoSoloIncluyeProyectosDelUsuarioAutenticado() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario viewer = crearUsuario("viewer");

        Proyecto propio = crearProyecto(owner);
        Proyecto ajeno = crearProyecto(viewer);

        asignarRol(propio, owner, Rol.OWNER);
        asignarRol(ajeno, viewer, Rol.OWNER);

        String token = jwtService.generateAccessToken(owner);

        mockMvc.perform(get("/proyectos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + propio.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + ajeno.getId() + ")]").doesNotExist());
    }

    @Test
    void parametroUsuarioIdNoPermiteConsultarProyectosAjenos() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario externo = crearUsuario("externo");

        Proyecto proyecto = crearProyecto(owner);
        asignarRol(proyecto, owner, Rol.OWNER);

        String token = jwtService.generateAccessToken(externo);

        mockMvc.perform(get("/proyectos")
                        .header("Authorization", "Bearer " + token)
                        .param("usuarioId", owner.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + proyecto.getId() + ")]").doesNotExist());
    }

    @Test
    void filtrosNoPermitenListarProyectosAjenos() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario externo = crearUsuario("externo");

        Proyecto proyecto = crearProyecto(owner);
        asignarRol(proyecto, owner, Rol.OWNER);

        String token = jwtService.generateAccessToken(externo);

        mockMvc.perform(get("/proyectos")
                        .header("Authorization", "Bearer " + token)
                        .param("nombre", "Proyecto RBAC")
                        .param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + proyecto.getId() + ")]").doesNotExist());
    }


    @Test
    void solicitudSinTokenEsRechazada() throws Exception {
        mockMvc.perform(get("/proyectos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitudConTokenInvalidoEsRechazada() throws Exception {
        mockMvc.perform(get("/proyectos")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitudConEsquemaDeAutenticacionIncorrectoEsRechazada()
            throws Exception {

        mockMvc.perform(get("/proyectos")
                        .header("Authorization", "Basic credenciales-invalidas"))
                .andExpect(status().isUnauthorized());
    }





}
