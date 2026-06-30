package cl.duoc.ms_login.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cl.duoc.ms_login.model.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generarToken(Integer id, String email, String nombre, Rol rol) {
        return Jwts.builder()
                .setSubject(email)
                .claim("id", id)
                .claim("nombre", nombre)
                .claim("rol", rol.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseClaimsJws(token)
            .getPayload();
    }

        public boolean esTokenValido(String token) {
        try {
            extraerClaims(token); // si no lanza excepcion, el token es valido
            return true;
        } catch (Exception e) {
            return false; // token expirado, firma incorrecta, o malformado
        }
    }
  
    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }
 
    public Integer extraerId(String token) {
        return extraerClaims(token).get("id", Integer.class);
    }
 
    public String extraerNombre(String token) {
        return extraerClaims(token).get("nombre", String.class);
    }
 
    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }
}
