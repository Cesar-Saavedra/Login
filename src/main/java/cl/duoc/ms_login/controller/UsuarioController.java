package cl.duoc.ms_login.controller;

// =============================================================
// ARCHIVO COMPLETO: UsuarioController.java  (ms-login)
// Agrega el endpoint GET /api/usuarios/{id} que necesita ms-tiendas
// para consultar el nombre del dueño de una tienda.
//
// Reemplaza tu UsuarioController.java actual con este.
// Solo se agrega un método al final, todo lo demás queda igual.
// =============================================================

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ms_login.dto.LoginRequestDto;
import cl.duoc.ms_login.dto.LoginResponseDto;
import cl.duoc.ms_login.dto.RegistroRequestDto;
import cl.duoc.ms_login.model.Usuario;
import cl.duoc.ms_login.security.JwtUtil;
import cl.duoc.ms_login.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    // ----------------------------------------------------------------
    // POST /api/usuarios/registro
    // ----------------------------------------------------------------
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequestDto dto) {
        try {
            LoginResponseDto respuesta = usuarioService.registrar(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    // ----------------------------------------------------------------
    // POST /api/usuarios/login
    // ----------------------------------------------------------------
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

    // ----------------------------------------------------------------
    // GET /api/usuarios/validar
    // Valida un JWT - usado por ms-tiendas, ms-grupos, ms-usuarios, etc.
    // ----------------------------------------------------------------
    @GetMapping("/validar")
    public ResponseEntity<?> validarToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> error = new HashMap<>();
            error.put("valido", false);
            error.put("error", "Header Authorization requerido. Formato: Bearer {token}");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.esTokenValido(token)) {
            Map<String, Object> error = new HashMap<>();
            error.put("valido", false);
            error.put("error", "Token invalido o expirado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("valido", true);
        respuesta.put("id",     jwtUtil.extraerId(token));
        respuesta.put("email",  jwtUtil.extraerEmail(token));
        respuesta.put("nombre", jwtUtil.extraerNombre(token));
        respuesta.put("rol",    jwtUtil.extraerRol(token));

        return ResponseEntity.ok(respuesta);
    }

    // ----------------------------------------------------------------
    // GET /api/usuarios/{id}          ← NUEVO ENDPOINT
    // Devuelve los datos básicos de un usuario por su id.
    // Lo necesita ms-tiendas para mostrar el nombre del dueño de una tienda.
    // También requiere token JWT válido para que no sea un endpoint abierto.
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verificar que quien consulta tiene un token válido
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token requerido."));
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.esTokenValido(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token invalido o expirado."));
        }

        // Buscar el usuario en la BD
        try {
            Usuario usuario = usuarioService.buscarPorId(id);

            // Devolver solo los datos necesarios (nunca devolver la contraseña)
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id",     usuario.getId());
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("email",  usuario.getEmail());
            respuesta.put("rol",    usuario.getRol().name());

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado: " + id));
        }
    }
}
