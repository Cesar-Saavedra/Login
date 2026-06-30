package cl.duoc.ms_login.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.ms_login.dto.LoginRequestDto;
import cl.duoc.ms_login.dto.LoginResponseDto;
import cl.duoc.ms_login.dto.RegistroRequestDto;
import cl.duoc.ms_login.model.Rol;
import cl.duoc.ms_login.model.Usuario;
import cl.duoc.ms_login.security.JwtUtil;
import cl.duoc.ms_login.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UsuarioController.class) // levanta la capa web, sin bd
public class UsuarioControllerTest {

    @Autowired
    private MockMvc llamadaFalsa; // sirve para crear llamadas html falsas

    @MockitoBean
    private UsuarioService service;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Usuario usuarioEjemplo;

    @BeforeEach
    void setUp(){

        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(1);
        usuarioEjemplo.setNombre("Nombre Prueba");
        usuarioEjemplo.setEmail("email prueba");
        usuarioEjemplo.setPassword("123456"); // Seteado para evitar NullPointerException si el service lo ocupa
        usuarioEjemplo.setRol(Rol.JUGADOR);    // 2. Aplicamos el rol aquí

    }

    @Test
    void buscarPorId_retorna200() throws Exception {

        // ARRANGE
        when(jwtUtil.esTokenValido("token-fake")).thenReturn(true);
        when(service.buscarPorId(1)).thenReturn(usuarioEjemplo);

        // ACT + ASSERT
        llamadaFalsa.perform(get("/api/usuarios/1")
                        .header("Authorization", "Bearer token-fake"))
                .andExpect(status().isOk());
    }

    // =====================================================================
    // POST /api/usuarios/registro
    // =====================================================================

    @Test
    void registro_retorna201() throws Exception {
        RegistroRequestDto dto = new RegistroRequestDto();
        dto.setNombre("Nombre Prueba");
        dto.setEmail("email@correo.com");
        dto.setPassword("123456");
        dto.setRol(Rol.JUGADOR);

        LoginResponseDto respuesta = new LoginResponseDto("token-fake", 1, "Nombre Prueba", "email@correo.com", Rol.JUGADOR);
        when(service.registrar(any(RegistroRequestDto.class))).thenReturn(respuesta);

        llamadaFalsa.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-fake"));
    }

    @Test
    void registro_emailDuplicado_retorna409() throws Exception {
        RegistroRequestDto dto = new RegistroRequestDto();
        dto.setNombre("Nombre Prueba");
        dto.setEmail("repetido@correo.com");
        dto.setPassword("123456");
        dto.setRol(Rol.JUGADOR);

        when(service.registrar(any(RegistroRequestDto.class)))
                .thenThrow(new RuntimeException("El email ya esta registrado: repetido@correo.com"));

        llamadaFalsa.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El email ya esta registrado: repetido@correo.com"));
    }

    // =====================================================================
    // POST /api/usuarios/login
    // =====================================================================

    @Test
    void login_retorna200() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("email@correo.com");
        dto.setPassword("123456");

        LoginResponseDto respuesta = new LoginResponseDto("token-fake", 1, "Nombre Prueba", "email@correo.com", Rol.JUGADOR);
        when(service.login(any(LoginRequestDto.class))).thenReturn(respuesta);

        llamadaFalsa.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-fake"));
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("email@correo.com");
        dto.setPassword("incorrecta");

        when(service.login(any(LoginRequestDto.class)))
                .thenThrow(new RuntimeException("Contrasena incorrecta"));

        llamadaFalsa.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Contrasena incorrecta"));
    }

    // =====================================================================
    // GET /api/usuarios/validar
    // =====================================================================

    @Test
    void validarToken_sinHeader_retorna401() throws Exception {
        llamadaFalsa.perform(get("/api/usuarios/validar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validarToken_tokenInvalido_retorna401() throws Exception {
        when(jwtUtil.esTokenValido("token-malo")).thenReturn(false);

        llamadaFalsa.perform(get("/api/usuarios/validar")
                        .header("Authorization", "Bearer token-malo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valido").value(false));
    }

    @Test
    void validarToken_tokenValido_retorna200() throws Exception {
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtUtil.extraerId("token-bueno")).thenReturn(1);
        when(jwtUtil.extraerEmail("token-bueno")).thenReturn("email@correo.com");
        when(jwtUtil.extraerNombre("token-bueno")).thenReturn("Nombre Prueba");
        when(jwtUtil.extraerRol("token-bueno")).thenReturn("JUGADOR");

        llamadaFalsa.perform(get("/api/usuarios/validar")
                        .header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.email").value("email@correo.com"));
    }

    // =====================================================================
    // GET /api/usuarios/{id}
    // =====================================================================

    @Test
    void obtenerUsuarioPorId_sinHeader_retorna401() throws Exception {
        llamadaFalsa.perform(get("/api/usuarios/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerUsuarioPorId_tokenInvalido_retorna401() throws Exception {
        when(jwtUtil.esTokenValido("token-malo")).thenReturn(false);

        llamadaFalsa.perform(get("/api/usuarios/1")
                        .header("Authorization", "Bearer token-malo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerUsuarioPorId_noEncontrado_retorna404() throws Exception {
        when(jwtUtil.esTokenValido("token-bueno")).thenReturn(true);
        when(service.buscarPorId(99)).thenThrow(new RuntimeException("Usuario no encontrado: 99"));

        llamadaFalsa.perform(get("/api/usuarios/99")
                        .header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isNotFound());
    }
}
