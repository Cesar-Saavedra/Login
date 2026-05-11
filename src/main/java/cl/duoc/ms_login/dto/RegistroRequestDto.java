package cl.duoc.ms_login.dto;
 
import cl.duoc.ms_login.model.Rol;
import lombok.Data;

@Data
public class RegistroRequestDto {
    private String nombre;
    private String email;
    private String password;
    private Rol rol;  // JUGADOR, TIENDA o ORGANIZADOR
}