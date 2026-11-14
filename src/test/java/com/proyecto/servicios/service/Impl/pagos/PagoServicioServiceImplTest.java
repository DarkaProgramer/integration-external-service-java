package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.exception.pagos.EstadoPagoInvalidoException;
import com.proyecto.servicios.exception.pagos.IdempotencyKeyInvalidaException;
import com.proyecto.servicios.exception.pagos.IdempotencyKeyReutilizadaException;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.ProductoNoDisponibleException;
import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.ResolucionManualRequestDto;
import com.proyecto.servicios.repositorys.pagos.PagoServicioRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.PagoServicioService.ResultadoPagoServicio;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ProveedorPagosGateway;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoServicioServiceImpl - orquestación del pago")
class PagoServicioServiceImplTest {

    private static final ClienteAutenticado CLIENTE = new ClienteAutenticado(1L, Set.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS));
    private static final ProductoCatalogo PRODUCTO = new ProductoCatalogo("1", "100", "Telcel $100", new BigDecimal("100"));

    @Mock private CatalogoProductosService catalogo;
    @Mock private ProveedorPagosGateway gateway;
    @Mock private PagoServicioTransacciones transacciones;
    @Mock private PagoServicioRepository pagoRepository;
    @Mock private com.proyecto.servicios.service.pagos.ValidacionReferenciaService validacionReferencia;

    private PagoServicioServiceImpl service;
    private String llave;
    private PagoServicioRequestDto solicitud;

    @BeforeEach
    void setUp() {
        service = new PagoServicioServiceImpl(catalogo, gateway, transacciones, pagoRepository, new PagoServicioMapper(), validacionReferencia);
        llave = UUID.randomUUID().toString();
        solicitud = new PagoServicioRequestDto();
        solicitud.setNumeroCuenta("1234567890");
        solicitud.setIdServicio("1");
        solicitud.setIdProducto("100");
        solicitud.setReferencia("5512345678");
        solicitud.setMonto(new BigDecimal("100"));
    }

    @Test
    void flujoCompleto_validaReservaEnviaYAplicaEnEseOrden() {
        PagoServicio pendiente = pago(EstatusPago.PENDIENTE);
        PagoServicio aprobado = pago(EstatusPago.APROBADA);
        when(pagoRepository.findByClienteIdAndIdempotencyKey(1L, llave)).thenReturn(Optional.empty());
        when(catalogo.validarProductoYMonto("1", "100", solicitud.getMonto())).thenReturn(PRODUCTO);
        when(transacciones.reservarFondos(eq(CLIENTE), eq(llave), anyString(), eq(solicitud), eq(PRODUCTO)))
                .thenReturn(new PagoServicioTransacciones.Reserva(pendiente, true));
        ResultadoProveedor ok = ResultadoProveedor.aprobada("GP-1", "0");
        when(gateway.enviarPago(any())).thenReturn(ok);
        when(transacciones.aplicarResultado(pendiente.getId(), ok)).thenReturn(aprobado);

        ResultadoPagoServicio resultado = service.pagar(CLIENTE, llave, solicitud);

        assertThat(resultado.repetida()).isFalse();
        assertThat(resultado.pago().getEstatus()).isEqualTo(EstatusPago.APROBADA);
        InOrder orden = inOrder(catalogo, transacciones, gateway);
        orden.verify(catalogo).validarProductoYMonto(any(), any(), any());
        orden.verify(transacciones).reservarFondos(any(), any(), any(), any(), any());
        orden.verify(gateway).enviarPago(any());
        orden.verify(transacciones).aplicarResultado(any(), any());
    }

    @Test
    void sinPermiso_noTocaNada() {
        ClienteAutenticado sinPermiso = new ClienteAutenticado(1L, Set.of());

        assertThatThrownBy(() -> service.pagar(sinPermiso, llave, solicitud)).isInstanceOf(OperacionNoAutorizadaException.class);
        verifyNoInteractions(catalogo, transacciones, gateway, pagoRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"})
    void llaveInvalida_400(String llaveInvalida) {
        assertThatThrownBy(() -> service.pagar(CLIENTE, llaveInvalida, solicitud)).isInstanceOf(IdempotencyKeyInvalidaException.class);
        verifyNoInteractions(transacciones, gateway);
    }

    @Test
    void llaveYaUsadaConMismosDatos_devuelveResultadoOriginalSinCobrarNiEnviar() {
        PagoServicio previo = pago(EstatusPago.APROBADA);
        previo.setHuellaSolicitud(PagoServicioServiceImpl.huella(solicitud));
        when(pagoRepository.findByClienteIdAndIdempotencyKey(1L, llave)).thenReturn(Optional.of(previo));

        ResultadoPagoServicio resultado = service.pagar(CLIENTE, llave, solicitud);

        assertThat(resultado.repetida()).isTrue();
        assertThat(resultado.pago().getFolio()).isEqualTo(previo.getFolio());
        verifyNoInteractions(catalogo, transacciones, gateway);
    }

    @Test
    void llaveYaUsadaConOtrosDatos_rechaza() {
        PagoServicio previo = pago(EstatusPago.APROBADA);
        previo.setHuellaSolicitud("otra-huella");
        when(pagoRepository.findByClienteIdAndIdempotencyKey(1L, llave)).thenReturn(Optional.of(previo));

        assertThatThrownBy(() -> service.pagar(CLIENTE, llave, solicitud)).isInstanceOf(IdempotencyKeyReutilizadaException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void carreraConLaMismaLlave_laRestriccionUnicaDejaPasarSoloUnaYLaOtraRepite() {
        PagoServicio ganador = pago(EstatusPago.APROBADA);
        ganador.setHuellaSolicitud(PagoServicioServiceImpl.huella(solicitud));
        when(pagoRepository.findByClienteIdAndIdempotencyKey(1L, llave))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(ganador));
        when(catalogo.validarProductoYMonto(any(), any(), any())).thenReturn(PRODUCTO);
        when(transacciones.reservarFondos(any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uq_pagos_cliente_idempotency"));

        ResultadoPagoServicio resultado = service.pagar(CLIENTE, llave, solicitud);

        assertThat(resultado.repetida()).isTrue();
        verify(gateway, never()).enviarPago(any());
    }

    @Test
    void productoInvalido_noReservaNiEnvia() {
        when(pagoRepository.findByClienteIdAndIdempotencyKey(1L, llave)).thenReturn(Optional.empty());
        when(catalogo.validarProductoYMonto(any(), any(), any())).thenThrow(new ProductoNoDisponibleException());

        assertThatThrownBy(() -> service.pagar(CLIENTE, llave, solicitud)).isInstanceOf(ProductoNoDisponibleException.class);
        verifyNoInteractions(transacciones, gateway);
    }

    @Test
    void huella_esIgualParaMontosEquivalentes() {
        String h1 = PagoServicioServiceImpl.huella(solicitud);
        solicitud.setMonto(new BigDecimal("100.00"));
        assertThat(PagoServicioServiceImpl.huella(solicitud)).isEqualTo(h1);
        solicitud.setReferencia("5500000000");
        assertThat(PagoServicioServiceImpl.huella(solicitud)).isNotEqualTo(h1);
    }

    @Test
    void resolucionManual_soloConPermisoYSoloPagosDudosos() {
        ResolucionManualRequestDto resolucion = new ResolucionManualRequestDto();
        resolucion.setFolio(UUID.randomUUID().toString());
        resolucion.setResultado("RECHAZADA");

        assertThatThrownBy(() -> service.resolverManualmente(CLIENTE, resolucion)).isInstanceOf(OperacionNoAutorizadaException.class);

        ClienteAutenticado operador = new ClienteAutenticado(9L, Set.of(ClienteAutenticado.PERMISO_CONCILIAR));
        when(pagoRepository.findByFolio(resolucion.getFolio())).thenReturn(Optional.of(pago(EstatusPago.APROBADA)));
        assertThatThrownBy(() -> service.resolverManualmente(operador, resolucion)).isInstanceOf(EstadoPagoInvalidoException.class);
        verify(transacciones, never()).aplicarResultado(any(), any());
    }

    private PagoServicio pago(EstatusPago estatus) {
        PagoServicio pago = new PagoServicio();
        pago.setId(10L);
        pago.setFolio(UUID.randomUUID().toString());
        pago.setIdServicio("1");
        pago.setIdProducto("100");
        pago.setReferencia("5512345678");
        pago.setMonto(new BigDecimal("100"));
        pago.setEstatus(estatus);
        return pago;
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Historial: solo del cliente, más reciente primero, con filtro por resultado")
    void historial() {
        PagoServicio pago = new PagoServicio();
        pago.setFolio("f1");
        pago.setEstatus(EstatusPago.APROBADA);
        pago.setReferencia("5512345678");
        pago.setMonto(new BigDecimal("100"));
        org.springframework.data.domain.Page<PagoServicio> pagina = new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(pago), org.springframework.data.domain.PageRequest.of(0, 20), 1);
        when(pagoRepository.findByClienteId(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(pagina);

        var resultado = service.historial(new ClienteAutenticado(1L, Set.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS)), null);
        assertThat(resultado.getTotalElementos()).isEqualTo(1);
        assertThat(resultado.getContenido()).singleElement().satisfies(p -> {
            assertThat(p.getResultado()).isEqualTo(com.proyecto.servicios.model.pagos.ResultadoPago.EXITOSO);
            assertThat(p.getReferencia()).isEqualTo("******5678");
        });

        when(pagoRepository.findByClienteIdAndEstatusIn(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(com.proyecto.servicios.model.pagos.ResultadoPago.PENDIENTE.estatus()), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        var filtros = new com.proyecto.servicios.model.pagos.HistorialPagosRequestDto(null, null,
                com.proyecto.servicios.model.pagos.ResultadoPago.PENDIENTE);
        assertThat(service.historial(new ClienteAutenticado(1L, Set.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS)), filtros)
                .getContenido()).isEmpty();

        assertThatThrownBy(() -> service.historial(new ClienteAutenticado(1L, Set.of()), null))
                .isInstanceOf(com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException.class);
    }
}
