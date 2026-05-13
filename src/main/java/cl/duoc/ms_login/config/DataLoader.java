package cl.duoc.ms_login.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cl.duoc.ms_login.model.Usuario;
import cl.duoc.ms_login.repository.UsuarioRepository;
import cl.duoc.ms_login.model.Rol;
@Configuration 
public class DataLoader {
    @Bean
    CommandLineRunner initData(UsuarioRepository usuRepo) {
        return args -> {

            Usuario tienda1 = new Usuario(null, "tienda1", "tienda1@example.com", "tienda1", Rol.TIENDA);
            Usuario tienda2 = new Usuario(null, "tienda2", "tienda2@example.com", "tienda2", Rol.TIENDA);
            Usuario tienda3 = new Usuario(null, "tienda3", "tienda3@example.com", "tienda3", Rol.TIENDA);
            Usuario jugador1 = new Usuario(null, "jugador1", "jugador1@example.com", "jugador1", Rol.JUGADOR);
            Usuario jugador2 = new Usuario(null, "jugador2", "jugador2@example.com", "jugador2", Rol.JUGADOR);
            Usuario jugador3 = new Usuario(null, "jugador3", "jugador3@example.com", "jugador3", Rol.JUGADOR);
            Usuario organizador1 = new Usuario(null, "organizador1", "organizador1@example.com", "organizador1", Rol.ORGANIZADOR);
            Usuario organizador2 = new Usuario(null, "organizador2", "organizador2@example.com", "organizador2", Rol.ORGANIZADOR);
            Usuario organizador3 = new Usuario(null, "organizador3", "organizador3@example.com", "organizador3", Rol.ORGANIZADOR);

            usuRepo.save(tienda1);
            usuRepo.save(tienda2);
            usuRepo.save(tienda3);
            usuRepo.save(jugador1);
            usuRepo.save(jugador2);
            usuRepo.save(jugador3);
            usuRepo.save(organizador1);
            usuRepo.save(organizador2);
            usuRepo.save(organizador3);

        }

    }

}