package com.safeandsound;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    // endpoint disponible únicamente durante los tests para comprobar que Spring Security protege correctamente las rutas autenticadas.
    @GetMapping("/test/protected")
    public ResponseEntity<String> protectedEndpoint() {
        return ResponseEntity.ok("Autenticado");
    }
}