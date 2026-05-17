package cl.duoc.ms_login.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import cl.duoc.ms_login.model.Rol;
import cl.duoc.ms_login.model.Usuario;
import cl.duoc.ms_login.repository.UsuarioRepository;

@Configuration
public class DataLoader {

    CommandLineRunner initData(UsuarioRepository usuarioRepository){
        return args -> {
            Usuario user1 = new Usuario();
            user1.setNombre("Ash Ketchum");
            user1.setEmail("ash@pueblopaleta.com");
            user1.setPassword("123456");
            user1.setRol(Rol.JUGADOR);

            Usuario user2 = new Usuario();
            user2.setNombre("Gary Oak");
            user2.setEmail("gary@pueblopaleta.com");
            user2.setPassword("123456");
            user2.setRol(Rol.JUGADOR);

            Usuario user3 = new Usuario();
            user3.setNombre("Admin Tienda Centro");
            user3.setEmail("admin@tiendacentro.com");
            user3.setPassword("admin123");
            user3.setRol(Rol.TIENDA);

            usuarioRepository.save(user1);
            usuarioRepository.save(user2);
            usuarioRepository.save(user3);
        };

    }
}
