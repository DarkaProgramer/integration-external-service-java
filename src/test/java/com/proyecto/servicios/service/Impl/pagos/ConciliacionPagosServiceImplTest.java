package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.repositorys.pagos.PagoServicioRepository;
import com.proyecto.servicios.service.pagos.ProveedorPagosGateway;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Conciliación de pagos dudosos")
class ConciliacionPagosServiceImplTest {

    @Mock private PagoServicioRepository pagoRepository;
    @Mock private PagoServicioTransacciones transacciones;
    @Mock private ProveedorPagosGateway gateway;

    private ConciliacionPagosServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConciliacionPagosServiceImpl(pagoRepository, transacciones, gateway, new PagosProperties(),
                Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC));
        when(pagoRepository.findIdsParaConciliar(any(), any())).thenReturn(List.of(1L));
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago()));
    }

    @Test
    void proveedorConfirma_aplicaElResultado() {
        ResultadoProveedor aprobada = ResultadoProveedor.aprobada("GP-1", "0");
        when(gateway.consultarEstado(any())).thenReturn(Optional.of(aprobada));

        assertThat(service.conciliarPendientes()).isEqualTo(1);
        verify(transacciones).aplicarResultado(1L, aprobada);
    }

    @Test
    void sinRespuestaConcluyente_soloRegistraIntento_nuncaReenviaNiDevuelve() {
        when(gateway.consultarEstado(any())).thenReturn(Optional.of(ResultadoProveedor.indeterminada("HTTP_503")));

        assertThat(service.conciliarPendientes()).isZero();
        verify(transacciones).registrarIntentoSinResultado(1L);
        verify(transacciones, never()).aplicarResultado(any(), any());
        verify(gateway, never()).enviarPago(any());
    }

    @Test
    void errorEnUnPago_noDetieneElLote() {
        when(pagoRepository.findIdsParaConciliar(any(), any())).thenReturn(List.of(1L, 2L));
        when(pagoRepository.findById(2L)).thenThrow(new IllegalStateException("x"));
        when(gateway.consultarEstado(any())).thenReturn(Optional.empty());

        service.conciliarPendientes();

        verify(transacciones).registrarIntentoSinResultado(1L);
    }

    private PagoServicio pago() {
        PagoServicio pago = new PagoServicio();
        pago.setId(1L);
        pago.setFolio("f");
        pago.setMonto(BigDecimal.TEN);
        pago.setEstatus(EstatusPago.EN_CONCILIACION);
        return pago;
    }
}
