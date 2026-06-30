package cl.duoc.ms_login.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import cl.duoc.ms_login.dto.LoginRequestDto;
import cl.duoc.ms_login.dto.LoginResponseDto;
import cl.duoc.ms_login.dto.RegistroRequestDto;
import cl.duoc.ms_login.model.Usuario;
import cl.duoc.ms_login.model.Rol;
import cl.duoc.ms_login.repository.UsuarioRepository;
import cl.duoc.ms_login.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioEjemplo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

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
    void buscarPorId_encontrado(){
        // ARRANGE
        Optional<Usuario> usuarioOptional = Optional.of(usuarioEjemplo);
        when(usuarioRepository.findById(1)).thenReturn(usuarioOptional);

        // ACT
        Usuario resultado = usuarioService.buscarPorId(1);

        // ASSERT (Corregido el typo de "ASSETS")
        assertEquals(1, resultado.getId());
        assertEquals("Nombre Prueba", resultado.getNombre());
        assertEquals(Rol.JUGADOR, resultado.getRol()); // 3. Validamos que el rol sea el correcto
    }

    @Test
    void buscarPorId_noEncontrado(){
        // ARRANGE
        Optional<Usuario> optionalVacio = Optional.empty();
        when(usuarioRepository.findById(99)).thenReturn(optionalVacio);

        // ACT & ASSERT
        RuntimeException error = assertThrows(RuntimeException.class, () -> {
            usuarioService.buscarPorId(99);
        });

        assertEquals("Usuario no encontrado: " + 99, error.getMessage());
    }

    // 4. Ejemplo extra: ¿Qué pasa si necesitas probar un rol distinto en un test específico?
    @Test
    void buscarPorId_usuarioEsTienda(){
        // ARRANGE: Cambiamos el rol solo para este test
        usuarioEjemplo.setRol(Rol.TIENDA);

        Optional<Usuario> usuarioOptional = Optional.of(usuarioEjemplo);
        when(usuarioRepository.findById(1)).thenReturn(usuarioOptional);

        // ACT
        Usuario resultado = usuarioService.buscarPorId(1);

        // ASSERT
        assertEquals(Rol.TIENDA, resultado.getRol());
    }

    // =====================================================================
    // buscarPorEmail
    // =====================================================================

    @Test
    void buscarPorEmail_encontrado(){
        when(usuarioRepository.findByEmail("email prueba")).thenReturn(Optional.of(usuarioEjemplo));

        Usuario resultado = usuarioService.buscarPorEmail("email prueba");

        assertEquals("Nombre Prueba", resultado.getNombre());
    }

    @Test
    void buscarPorEmail_noEncontrado(){
        when(usuarioRepository.findByEmail("inexistente@correo.com")).thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                usuarioService.buscarPorEmail("inexistente@correo.com"));

        assertEquals("Email no encontrado: inexistente@correo.com", error.getMessage());
    }

    // =====================================================================
    // eliminarPorId
    // =====================================================================

    @Test
    void eliminarPorId_invocaRepository(){
        usuarioService.eliminarPorId(1);

        verify(usuarioRepository).deleteById(1);
    }

    // =====================================================================
    // registrar
    // =====================================================================

    @Test
    void registrar_exitoso(){
        RegistroRequestDto dto = new RegistroRequestDto();
        dto.setNombre("Nuevo Usuario");
        dto.setEmail("nuevo@correo.com");
        dto.setPassword("123456");
        dto.setRol(Rol.JUGADOR);

        when(usuarioRepository.existsByEmail("nuevo@correo.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario u = invocacion.getArgument(0);
            u.setId(10);
            return u;
        });
        when(jwtUtil.generarToken(10, "nuevo@correo.com", "Nuevo Usuario", Rol.JUGADOR))
                .thenReturn("token-fake");

        LoginResponseDto respuesta = usuarioService.registrar(dto);

        assertNotNull(respuesta);
        assertEquals("token-fake", respuesta.getToken());
        assertEquals(10, respuesta.getId());
        assertEquals("nuevo@correo.com", respuesta.getEmail());
    }

    @Test
    void registrar_emailYaRegistrado(){
        RegistroRequestDto dto = new RegistroRequestDto();
        dto.setNombre("Nuevo Usuario");
        dto.setEmail("repetido@correo.com");
        dto.setPassword("123456");
        dto.setRol(Rol.JUGADOR);

        when(usuarioRepository.existsByEmail("repetido@correo.com")).thenReturn(true);

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                usuarioService.registrar(dto));

        assertEquals("El email ya esta registrado: repetido@correo.com", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    // =====================================================================
    // login
    // =====================================================================

    @Test
    void login_exitoso(){
        Usuario usuarioConHash = new Usuario();
        usuarioConHash.setId(1);
        usuarioConHash.setNombre("Nombre Prueba");
        usuarioConHash.setEmail("email@correo.com");
        usuarioConHash.setPassword(encoder.encode("123456"));
        usuarioConHash.setRol(Rol.JUGADOR);

        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("email@correo.com");
        dto.setPassword("123456");

        when(usuarioRepository.findByEmail("email@correo.com")).thenReturn(Optional.of(usuarioConHash));
        when(jwtUtil.generarToken(1, "email@correo.com", "Nombre Prueba", Rol.JUGADOR))
                .thenReturn("token-fake");

        LoginResponseDto respuesta = usuarioService.login(dto);

        assertEquals("token-fake", respuesta.getToken());
        assertEquals("email@correo.com", respuesta.getEmail());
    }

    @Test
    void login_emailNoEncontrado(){
        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("inexistente@correo.com");
        dto.setPassword("123456");

        when(usuarioRepository.findByEmail("inexistente@correo.com")).thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                usuarioService.login(dto));

        assertEquals("Email no encontrado: inexistente@correo.com", error.getMessage());
    }

    @Test
    void login_contrasenaIncorrecta(){
        Usuario usuarioConHash = new Usuario();
        usuarioConHash.setId(1);
        usuarioConHash.setNombre("Nombre Prueba");
        usuarioConHash.setEmail("email@correo.com");
        usuarioConHash.setPassword(encoder.encode("123456"));
        usuarioConHash.setRol(Rol.JUGADOR);

        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("email@correo.com");
        dto.setPassword("clave-incorrecta");

        when(usuarioRepository.findByEmail("email@correo.com")).thenReturn(Optional.of(usuarioConHash));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                usuarioService.login(dto));

        assertEquals("Contrasena incorrecta", error.getMessage());
    }
}
