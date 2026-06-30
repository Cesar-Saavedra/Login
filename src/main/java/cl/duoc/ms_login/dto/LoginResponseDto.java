package cl.duoc.ms_login.dto;

import cl.duoc.ms_login.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {
    private String token;   // JWT con duracion de 24 horas
    private Integer id;
    private String nombre;
    private String email;
    private Rol rol;
}