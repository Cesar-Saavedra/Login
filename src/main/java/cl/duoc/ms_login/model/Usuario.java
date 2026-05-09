package cl.duoc.ms_login.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    
    @Column(nullable = false)
    private Integer id;
    
    @Column(nullable = false)
    private String nombre;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
}
