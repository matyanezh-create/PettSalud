package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.*;
import com.petsalud.backendpetsalud.repository.*;
import com.petsalud.backendpetsalud.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository repositorio;
    @InjectMocks UsuarioServiceImpl servicio;

    static Usuario usuarioValido() {
        return Usuario.builder().id(7L).nombre("Ana").apellido("Soto")
                .email("ana@example.test").direccion("Av. Central 10")
                .rol(Rol.CLIENTE).password("dato-de-prueba").build();
    }

    static Stream<Arguments> camposIncompletos() {
        return Stream.of(
                Arguments.of("nombre", (Consumer<Usuario>) u -> u.setNombre(null)),
                Arguments.of("apellido", (Consumer<Usuario>) u -> u.setApellido(" ")),
                Arguments.of("email", (Consumer<Usuario>) u -> u.setEmail("")),
                Arguments.of("dirección", (Consumer<Usuario>) u -> u.setDireccion(null))
        );
    }

    @ParameterizedTest(name = "Rechaza registro si falta {0}")
    @MethodSource("camposIncompletos")
    void rechazaRegistroIncompleto(String campo, Consumer<Usuario> quitarCampo) {
        Usuario usuario = usuarioValido();
        quitarCampo.accept(usuario);

        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrarUsuario(usuario));

        assertEquals("Falta el campo " + campo, error.getMessage());
        verifyNoInteractions(repositorio);
    }

    @Test
    void registraUsuarioCompletoConEmailDisponible() {
        Usuario usuario = usuarioValido();
        when(repositorio.findByEmail(usuario.getEmail())).thenReturn(Optional.empty());
        when(repositorio.save(usuario)).thenReturn(usuario);

        assertSame(usuario, servicio.registrarUsuario(usuario));

        var orden = inOrder(repositorio);
        orden.verify(repositorio).findByEmail(usuario.getEmail());
        orden.verify(repositorio).save(usuario);
    }

    @Test
    void rechazaEmailDuplicado() {
        Usuario usuario = usuarioValido();
        when(repositorio.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrarUsuario(usuario));

        assertEquals("El email ya está registrado", error.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizaDatosEditablesConservandoEmailYClave() {
        Usuario original = usuarioValido();
        Usuario cambios = Usuario.builder().nombre("Eva").apellido("Paz").telefono("555")
                .direccion("Av. Sur 9").email("otro@example.test").password("otra").build();
        when(repositorio.findById(7L)).thenReturn(Optional.of(original));
        when(repositorio.save(original)).thenReturn(original);

        Usuario resultado = servicio.actualizarUsuario(7L, cambios);

        assertAll(
                () -> assertEquals("Eva", resultado.getNombre()),
                () -> assertEquals("Paz", resultado.getApellido()),
                () -> assertEquals("555", resultado.getTelefono()),
                () -> assertEquals("Av. Sur 9", resultado.getDireccion()),
                () -> assertEquals("ana@example.test", resultado.getEmail()),
                () -> assertEquals("dato-de-prueba", resultado.getPassword())
        );
        verify(repositorio).save(original);
    }

    @Test
    void rechazaActualizarUsuarioInexistente() {
        when(repositorio.findById(7L)).thenReturn(Optional.empty());

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarUsuario(7L, usuarioValido()));

        assertEquals("Usuario no encontrado", error.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void consultaUsuarioPorIdYPorEmail() {
        Usuario usuario = usuarioValido();
        when(repositorio.findById(7L)).thenReturn(Optional.of(usuario));
        when(repositorio.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        assertSame(usuario, servicio.obtenerUsuarioPorId(7L).orElseThrow());
        assertSame(usuario, servicio.obtenerUsuarioPorEmail(usuario.getEmail()).orElseThrow());
    }

    @Test
    void listaYEliminaUsuarios() {
        Usuario usuario = usuarioValido();
        when(repositorio.findAll()).thenReturn(List.of(usuario));

        assertEquals(List.of(usuario), servicio.listarUsuarios());
        servicio.eliminarUsuario(7L);

        verify(repositorio).deleteById(7L);
    }
}
