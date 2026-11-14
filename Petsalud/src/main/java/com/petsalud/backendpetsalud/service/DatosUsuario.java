package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Usuario;

final class DatosUsuario {

    private DatosUsuario() {
    }

    static void validar(Usuario usuario) {
        obligatorio("nombre", usuario.getNombre());
        obligatorio("apellido", usuario.getApellido());
        obligatorio("email", usuario.getEmail());
        obligatorio("dirección", usuario.getDireccion());
    }

    private static void obligatorio(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el campo " + campo);
        }
    }
}
