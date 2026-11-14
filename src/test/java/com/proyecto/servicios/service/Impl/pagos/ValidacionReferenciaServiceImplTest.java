package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.entity.pagos.ValidacionReferencia;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.ReferenciaInvalidaException;
import com.proyecto.servicios.exception.pagos.ReferenciaNoValidadaException;
import com.proyecto.servicios.exception.pagos.ValidacionNoDisponibleException;
import com.proyecto.servicios.exception.pagos.ValidacionYaUsadaException;
import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.ValidarReferenciaRequestDto;
import com.proyecto.servicios.repositorys.pagos.ValidacionReferenciaRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ReglaReferencia;
import com.proyecto.servicios.service.pagos.ResultadoValidacionReferencia;
import com.proyecto.servicios.service.pagos.ValidadorReferenciaGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidacionReferenciaServiceImpl")
class ValidacionReferenciaServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC);
    private static final ClienteAutenticado CLIENTE = new ClienteAutenticado(1L, Set.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS));
    private static final ProductoCatalogo CFE = new ProductoCatalogo("5", "500", "Pago CFE", BigDecimal.ZERO, "CFE", "Luz",
            "Luz", null, new ReglaReferencia(12, 12, true));

    @Mock private CatalogoProductosService catalogo;
    @Mock private ValidadorReferenciaGateway gateway;
    @Mock private ValidacionReferenciaRepository repository;

    private ValidacionReferenciaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ValidacionReferenciaServiceImpl(catalogo, gateway, repository, CLOCK, 10);
    }

    @Test
    @DisplayName("Referencia válida: guarda el comprobante con vigencia y devuelve la referencia enmascarada")
    void valida() {
        when(catalogo.obtener("5", "500")).thenReturn(CFE);
        when(gateway.validar(CFE, "123456789012")).thenReturn(ResultadoValidacionReferencia.valida(new BigDecimal("345.50"), "0"));

        var respuesta = service.validar(CLIENTE, new ValidarReferenciaRequestDto("5", "500", " 123456789012 "));

        ArgumentCaptor<ValidacionReferencia> guardada = ArgumentCaptor.forClass(ValidacionReferencia.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getExpiraEn()).isEqualTo(LocalDateTime.now(CLOCK).plusMinutes(10));
        assertThat(respuesta.getValidacionId()).isEqualTo(guardada.getValue().getId());
        assertThat(respuesta.getReferencia()).isEqualTo("********9012");
        assertThat(respuesta.getMontoAdeudo()).isEqualByComparingTo("345.50");
        assertThat(respuesta.getPrecio()).isNull();
    }

    @Test
    @DisplayName("Formato inválido (no consulta al proveedor), rechazo del proveedor 422, proveedor caído 503, sin permiso 403")
    void errores() {
        when(catalogo.obtener("5", "500")).thenReturn(CFE);
        assertThatThrownBy(() -> service.validar(CLIENTE, new ValidarReferenciaRequestDto("5", "500", "123")))
                .isInstanceOf(ReferenciaInvalidaException.class).hasMessageContaining("exactamente 12");
        verify(gateway, never()).validar(any(), any());

        when(gateway.validar(any(), any())).thenReturn(ResultadoValidacionReferencia.invalida("12"));
        assertThatThrownBy(() -> service.validar(CLIENTE, new ValidarReferenciaRequestDto("5", "500", "123456789012")))
                .isInstanceOf(ReferenciaInvalidaException.class);
        when(gateway.validar(any(), any())).thenReturn(ResultadoValidacionReferencia.noDisponible("HTTP_503"));
        assertThatThrownBy(() -> service.validar(CLIENTE, new ValidarReferenciaRequestDto("5", "500", "123456789012")))
                .isInstanceOf(ValidacionNoDisponibleException.class);
        verify(repository, never()).save(any());

        assertThatThrownBy(() -> service.validar(new ClienteAutenticado(1L, Set.of()), new ValidarReferenciaRequestDto("5", "500", "1")))
                .isInstanceOf(OperacionNoAutorizadaException.class);
    }

    @Test
    @DisplayName("Para pagar: debe existir, ser del cliente, coincidir, no estar usada ni vencida")
    void verificarParaPago() {
        PagoServicioRequestDto pago = new PagoServicioRequestDto();
        pago.setIdServicio("5");
        pago.setIdProducto("500");
        pago.setReferencia("123456789012");
        pago.setValidacionId("ABC");

        when(repository.findByIdAndClienteId("abc", 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verificarParaPago(CLIENTE, pago)).isInstanceOf(ReferenciaNoValidadaException.class);

        ValidacionReferencia v = new ValidacionReferencia();
        v.setIdServicio("5");
        v.setIdProducto("500");
        v.setReferencia("999999999999");
        v.setExpiraEn(LocalDateTime.now(CLOCK).plusMinutes(1));
        when(repository.findByIdAndClienteId("abc", 1L)).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.verificarParaPago(CLIENTE, pago)).isInstanceOf(ReferenciaNoValidadaException.class);

        v.setReferencia("123456789012");
        service.verificarParaPago(CLIENTE, pago);

        v.setExpiraEn(LocalDateTime.now(CLOCK));
        assertThatThrownBy(() -> service.verificarParaPago(CLIENTE, pago)).isInstanceOf(ReferenciaNoValidadaException.class);

        v.setUsadaEn(LocalDateTime.now(CLOCK));
        assertThatThrownBy(() -> service.verificarParaPago(CLIENTE, pago)).isInstanceOf(ValidacionYaUsadaException.class);

        pago.setValidacionId(null);
        when(repository.findByIdAndClienteId("", 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verificarParaPago(CLIENTE, pago)).isInstanceOf(ReferenciaNoValidadaException.class);
    }
}
