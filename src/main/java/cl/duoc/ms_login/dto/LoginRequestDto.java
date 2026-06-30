package cl.duoc.ms_login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}