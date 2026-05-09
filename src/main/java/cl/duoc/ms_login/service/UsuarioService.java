package cl.duoc.ms_login.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.duoc.ms_login.model.Usuario;
import cl.duoc.ms_login.repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    //guardar usuario
    public Usuario guardarUsuario(Usuario usuario){
        return usuarioRepository.save(usuario);
    }

    //eliminar usuario por id
    public void eliminarUsuarioPorId(Integer id){
        usuarioRepository.deleteById(id);
    }

    //buscar usuario por id
    public Usuario buscarUsuarioPorId(Integer id){
        return usuarioRepository.findById(id).orElse(null);
    }


}
