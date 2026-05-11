package cl.duoc.ms_login.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import cl.duoc.ms_login.dto.LoginRequestDto;
import cl.duoc.ms_login.dto.LoginResponseDto;
import cl.duoc.ms_login.dto.RegistroRequestDto;
import cl.duoc.ms_login.model.Usuario;
import cl.duoc.ms_login.repository.UsuarioRepository;
import cl.duoc.ms_login.security.JwtUtil;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // BCryptPasswordEncoder: hashea la contrasena antes de guardarla en BD
    // La "fortaleza" (10) determina cuantas rondas de hash se aplican.
    // Mas rondas = mas seguro pero mas lento. 10 es el estandar.
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    // =====================================================================
    // REGISTRO
    // =====================================================================

    /**
     * Registra un nuevo usuario en CardLink.
     *
     * Proceso:
     * 1. Verifica que el email no este ya registrado.
     * 2. Hashea la contrasena con BCrypt.
     * 3. Guarda el usuario en la BD.
     * 4. Genera y devuelve un JWT (el usuario ya queda logueado).
     *
     * @throws RuntimeException si el email ya existe
     */
    public LoginResponseDto registrar(RegistroRequestDto dto) {
        // Paso 1: verificar que el email no este en uso
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El email ya esta registrado: " + dto.getEmail());
        }

        // Paso 2: crear el usuario con contrasena hasheada
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(encoder.encode(dto.getPassword())); // NUNCA guardar en texto plano
        usuario.setRol(dto.getRol());

        // Paso 3: guardar en BD
        Usuario guardado = usuarioRepository.save(usuario);

        // Paso 4: generar JWT y devolver respuesta
        String token = jwtUtil.generarToken(
                guardado.getId(),
                guardado.getEmail(),
                guardado.getNombre(),
                guardado.getRol()
        );

        return new LoginResponseDto(
                token,
                guardado.getId(),
                guardado.getNombre(),
                guardado.getEmail(),
                guardado.getRol()
        );
    }

    // =====================================================================
    // LOGIN
    // =====================================================================

    /**
     * Autentica un usuario y devuelve un JWT.
     *
     * Proceso:
     * 1. Busca al usuario por email.
     * 2. Verifica que la contrasena ingresada coincida con el hash en BD.
     *    (BCrypt compara de forma segura, no desencripta).
     * 3. Genera y devuelve un JWT valido por 24 horas.
     *
     * @throws RuntimeException si el email no existe o la contrasena es incorrecta
     */
    public LoginResponseDto login(LoginRequestDto dto) {
        // Paso 1: buscar usuario por email
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Email no encontrado: " + dto.getEmail()));

        // Paso 2: verificar contrasena
        // encoder.matches() compara la contrasena en texto plano con el hash guardado
        if (!encoder.matches(dto.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Contrasena incorrecta");
        }

        // Paso 3: generar JWT
        String token = jwtUtil.generarToken(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol()
        );

        return new LoginResponseDto(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol()
        );
    }

    // =====================================================================
    // METODOS DE APOYO (para uso interno y otros microservicios)
    // =====================================================================

    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email no encontrado: " + email));
    }

    public void eliminarPorId(Integer id) {
        usuarioRepository.deleteById(id);
    }
}