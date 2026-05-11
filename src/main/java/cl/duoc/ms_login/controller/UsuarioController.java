package cl.duoc.ms_login.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ms_login.dto.LoginRequestDto;
import cl.duoc.ms_login.dto.LoginResponseDto;
import cl.duoc.ms_login.dto.RegistroRequestDto;
import cl.duoc.ms_login.security.JwtUtil;
import cl.duoc.ms_login.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequestDto registroDTO) {
        try {
            LoginResponseDto respuesta = usuarioService.registrar(registroDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (RuntimeException e) {
            // El email ya existe u otro error de validacion
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody LoginRequestDto dto) {
            try {
                LoginResponseDto respuesta = usuarioService.login(dto);
                return ResponseEntity.ok(respuesta);
            } catch (RuntimeException e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }        
    }


    @GetMapping("/validar")
    public ResponseEntity<?> validarToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
 
        // Verificar que el header exista y tenga el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> error = new HashMap<>();
            error.put("valido", false);
            error.put("error", "Header Authorization requerido. Formato: Bearer {token}");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
 
        // Extraer el token (remover el prefijo "Bearer ")
        String token = authHeader.substring(7);
 
        // Validar el token
        if (!jwtUtil.esTokenValido(token)) {
            Map<String, Object> error = new HashMap<>();
            error.put("valido", false);
            error.put("error", "Token invalido o expirado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
 
        // Token valido: extraer y devolver los datos del usuario
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("valido", true);
        respuesta.put("id", jwtUtil.extraerId(token));
        respuesta.put("email", jwtUtil.extraerEmail(token));
        respuesta.put("nombre", jwtUtil.extraerNombre(token));
        respuesta.put("rol", jwtUtil.extraerRol(token));
 
        return ResponseEntity.ok(respuesta);
    }        
}
// Explicandole Al yoshua como lo estamos haciendo
