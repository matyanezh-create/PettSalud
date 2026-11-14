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

import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;

import static com.petsalud.backendpetsalud.UsuarioServiceTest.usuarioValido;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock UsuarioRepository usuarios;
    @Mock ProductoRepository productos;
    @Mock VentaRepository ventas;
    @InjectMocks VentaServiceImpl servicio;

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ADMIN", "CLIENTE"})
    void aceptaRolAutorizadoYConservaRelaciones(Rol rol) {
        Usuario comprador = usuarioValido();
        comprador.setRol(rol);
        Producto alimento = producto(1L, "1290.50", 2);
        Producto champu = producto(2L, "999.99", 1);
        List<Long> ids = List.of(1L, 2L);
        when(usuarios.findById(7L)).thenReturn(Optional.of(comprador));
        when(productos.buscarParaVenta(ids)).thenReturn(List.of(alimento, champu));
        when(ventas.save(any(Venta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Venta venta = servicio.realizarVenta(7L, ids);

        var captor = ArgumentCaptor.forClass(Venta.class);
        var orden = inOrder(productos, ventas);
        orden.verify(productos).save(alimento);
        orden.verify(productos).save(champu);
        orden.verify(ventas).save(captor.capture());
        assertAll(
                () -> assertSame(captor.getValue(), venta),
                () -> assertSame(comprador, venta.getUsuario()),
                () -> assertEquals(2, venta.getProductos().size()),
                () -> assertSame(alimento, venta.getProductos().get(0)),
                () -> assertSame(champu, venta.getProductos().get(1)),
                () -> assertEquals(new BigDecimal("2290.49"), venta.getTotal()),
                () -> assertEquals(1, alimento.getStock()),
                () -> assertEquals(0, champu.getStock()),
                () -> assertNotNull(venta.getFecha())
        );
    }

    @ParameterizedTest
    @MethodSource("com.petsalud.backendpetsalud.UsuarioServiceTest#camposIncompletos")
    void rechazaCompradorIncompleto(String campo, Consumer<Usuario> quitarCampo) {
        Usuario comprador = usuarioValido();
        quitarCampo.accept(comprador);
        when(usuarios.findById(7L)).thenReturn(Optional.of(comprador));

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.realizarVenta(7L, List.of(1L)));

        assertEquals("Falta el campo " + campo, error.getMessage());
        verifyNoInteractions(productos, ventas);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = Rol.class, names = "VETERINARIO")
    void rechazaRolSinPermiso(Rol rol) {
        Usuario comprador = usuarioValido();
        comprador.setRol(rol);
        when(usuarios.findById(7L)).thenReturn(Optional.of(comprador));

        assertThrows(AccessDeniedException.class, () -> servicio.realizarVenta(7L, List.of(1L)));

        verifyNoInteractions(productos, ventas);
    }

    @Test
    void rechazaUsuarioInexistente() {
        when(usuarios.findById(7L)).thenReturn(Optional.empty());

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.realizarVenta(7L, List.of(1L)));

        assertEquals("Usuario no encontrado", error.getMessage());
        verifyNoInteractions(productos, ventas);
    }

    @Test
    void noDescuentaStockCuandoElUltimoProductoEstaAgotado() {
        List<Long> ids = List.of(1L, 2L);
        Producto disponible = producto(1L, "1250.00", 3);
        Producto agotado = producto(2L, "999.99", 0);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuarioValido()));
        when(productos.buscarParaVenta(ids)).thenReturn(List.of(disponible, agotado));

        var error = assertThrows(IllegalArgumentException.class, () -> servicio.realizarVenta(7L, ids));

        assertEquals("El producto Producto 2 no tiene stock disponible", error.getMessage());
        assertEquals(3, disponible.getStock());
        assertEquals(0, agotado.getStock());
        verify(productos, never()).save(any());
        verifyNoInteractions(ventas);
    }

    @Test
    void noRegistraVentaCuandoFaltaUnProducto() {
        List<Long> ids = List.of(1L, 2L);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuarioValido()));
        when(productos.buscarParaVenta(ids)).thenReturn(List.of(producto(1L, "1000", 1)));

        var error = assertThrows(IllegalArgumentException.class, () -> servicio.realizarVenta(7L, ids));

        assertEquals("Algunos productos no fueron encontrados", error.getMessage());
        verify(productos, never()).save(any());
        verifyNoInteractions(ventas);
    }

    static Stream<List<Long>> listasInvalidas() {
        return Stream.of(null, List.of(), Collections.singletonList(null), List.of(1L, 1L));
    }

    @ParameterizedTest
    @MethodSource("listasInvalidas")
    void rechazaListadoInvalidoAntesDeConsultarProductos(List<Long> ids) {
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuarioValido()));

        assertThrows(IllegalArgumentException.class, () -> servicio.realizarVenta(7L, ids));

        verifyNoInteractions(productos, ventas);
    }

    @Test
    void consultaVentasPorIdYUsuarioYListaTodas() {
        Usuario comprador = usuarioValido();
        Venta venta = Venta.builder().id(10L).usuario(comprador).build();
        when(ventas.findById(10L)).thenReturn(Optional.of(venta));
        when(ventas.findByUsuarioId(7L)).thenReturn(List.of(venta));
        when(ventas.findAll()).thenReturn(List.of(venta));

        assertSame(venta, servicio.obtenerVentaPorId(10L).orElseThrow());
        assertTrue(servicio.obtenerVentaPorId(11L).isEmpty());
        assertEquals(List.of(venta), servicio.listarVentasPorUsuario(7L));
        assertEquals(List.of(venta), servicio.listarTodasLasVentas());
    }

    private Producto producto(long id, String precio, int stock) {
        return Producto.builder().id(id).nombre("Producto " + id)
                .precio(new BigDecimal(precio)).stock(stock).build();
    }
}
