package com.safeandsound;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;


    //comprueba el flujo completo de Spring Security: un JWT inválido en un endpoint protegido debe devolver 401.
    @Test
    void endpointProtegidoConTokenInvalidoDebeResponder401() throws Exception {

        mockMvc.perform(
                        get("/test/protected")
                                .header("Authorization", "Bearer token-invalido")
                )
                .andExpect(status().isUnauthorized());
    }
}