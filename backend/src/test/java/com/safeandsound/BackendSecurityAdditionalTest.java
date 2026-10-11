package com.safeandsound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.UsuarioRepository;
import com.safeandsound.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BackendSecurityAdditionalTests {
    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Usuario usuario() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Usuario u = new Usuario();
        u.setUsername("sec_" + id);
        u.setEmail("sec_" + id + "@test.com");
        u.setPasswordHash("hash-de-prueba");
        return usuarioRepository.saveAndFlush(u);
    }

    @Test void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/proyectos")).andExpect(status().isUnauthorized());
    }
    @Test void tokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/proyectos").header("Authorization", "Bearer no-es-jwt"))
                .andExpect(status().isUnauthorized());
    }
    @Test void esquemaBasicDevuelve401() throws Exception {
        mockMvc.perform(get("/proyectos").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
    }
    @Test void tokenAlteradoDevuelve401() throws Exception {
        String jwt = jwtService.generateAccessToken(usuario());
        String[] partes = jwt.split("\\.");
        String firma = partes[2];
        char primero = firma.charAt(0);
        partes[2] = (primero == 'A' ? 'B' : 'A') + firma.substring(1);
        mockMvc.perform(get("/proyectos").header("Authorization", "Bearer " + String.join(".", partes)))
                .andExpect(status().isUnauthorized());
    }
    @Test void refreshNoSirveComoAccess() throws Exception {
        String refresh = jwtService.generateRefreshToken(usuario());
        mockMvc.perform(get("/proyectos").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }
    @Test void tokenDeUsuarioEliminadoDevuelve401() throws Exception {
        Usuario u = usuario();
        String token = jwtService.generateAccessToken(u);
        usuarioRepository.delete(u);
        usuarioRepository.flush();
        mockMvc.perform(get("/proyectos").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
    @Test void accessNoSirveParaRefresh() throws Exception {
        String access = jwtService.generateAccessToken(usuario());
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", access))))
                .andExpect(status().is4xxClientError());
    }
    @Test void refreshInventadoNoSeAcepta() throws Exception {
        String refresh = jwtService.generateRefreshToken(usuario());
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }
    @Test void logoutDeRefreshNoRegistradoDevuelve401() throws Exception {
        String refresh = jwtService.generateRefreshToken(usuario());
        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }
    @Test void registroInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalido\",\"username\":\"a!\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void loginSinPasswordDevuelve400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"prueba@test.com\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void refreshVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void logoutSinTokenDevuelve400() throws Exception {
        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
    @Test void tokenValidoPermiteListarProyectos() throws Exception {
        String token = jwtService.generateAccessToken(usuario());
        mockMvc.perform(get("/proyectos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
