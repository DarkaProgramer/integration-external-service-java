package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.entity.pagos.MovimientoCuenta;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.entity.pagos.TipoMovimiento;
import com.proyecto.servicios.exception.pagos.ClienteInactivoException;
import com.proyecto.servicios.exception.pagos.CuentaNoOperableException;
import com.proyecto.servicios.exception.pagos.LimiteDiarioExcedidoException;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.SaldoInsuficienteException;
import com.proyecto.servicios.exception.pagos.SolicitudDuplicadaException;
import com.proyecto.servicios.exception.pagos.ValidacionYaUsadaException;
import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.pagos.MovimientoCuentaRepository;
import com.proyecto.servicios.repositorys.pagos.PagoServicioRepository;
import com.proyecto.servicios.repositorys.pagos.ValidacionReferenciaRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.AutorizacionTarjeta;
import com.proyecto.servicios.service.pagos.ProcesadorTarjetas;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Pasos transaccionales (cortos) del pago. Están en un bean aparte para que {@link Transactional} aplique
 * (el proxy de Spring no intercepta llamadas internas) y para que la llamada HTTP al proveedor quede
 * <b>fuera</b> de cualquier transacción: así no se mantiene bloqueada la cuenta mientras se espera a Gestopago.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagoServicioTransacciones {

    static final String CUENTA_ACTIVA = "ACTIVA";
    /** Pagos que cuentan para duplicados y límite diario: todos menos los rechazados (cuyo dinero ya se devolvió). */
    static final Set<EstatusPago> ESTATUS_VIGENTES =
            EnumSet.of(EstatusPago.PENDIENTE, EstatusPago.APROBADA, EstatusPago.EN_CONCILIACION, EstatusPago.REVISION_MANUAL);

    private final CuentaRepository cuentaRepository;
    private final PagoServicioRepository pagoRepository;
    private final MovimientoCuentaRepository movimientoRepository;
    private final ValidacionReferenciaRepository validacionRepository;
    private final PagosProperties properties;
    private final Clock clock;
    private final ProcesadorTarjetas procesadorTarjetas;

    static final String METODO_TARJETA = "TARJETA_DEBITO";
    static final String METODO_SALDO = "SALDO";

    /**
     * Paso 1: valida cliente y cuenta, descuenta el saldo (o asocia el cargo ya autorizado a la tarjeta de débito)
     * y registra el pago PENDIENTE, todo en una transacción
     * con la fila de la cuenta bloqueada (SELECT ... FOR UPDATE). Si algo falla, no queda cargo ni registro.
     *
     * @return el pago existente si otra petición con la misma llave se adelantó, o el nuevo pago
     */
    @Transactional
    public Reserva reservarFondos(ClienteAutenticado cliente, String idempotencyKey, String huella,
                                  PagoServicioRequestDto solicitud, ProductoCatalogo producto,
                                  AutorizacionTarjeta tarjeta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaParaActualizar(solicitud.getNumeroCuenta())
                .orElseThrow(OperacionNoAutorizadaException::new);
        validarTitularYEstado(cliente, cuenta);

        // Con la cuenta bloqueada, una petición concurrente con la misma llave ya terminó o está esperando
        Optional<PagoServicio> previo = pagoRepository.findByClienteIdAndIdempotencyKey(cliente.clienteId(), idempotencyKey);
        if (previo.isPresent()) {
            return new Reserva(previo.get(), false);
        }

        BigDecimal monto = solicitud.getMonto();
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (pagoRepository.existeSimilarReciente(cuenta.getId(), solicitud.getIdServicio(), solicitud.getIdProducto(),
                solicitud.getReferencia(), monto, ESTATUS_VIGENTES, ahora.minusSeconds(properties.getVentanaDuplicadosSegundos()))) {
            throw new SolicitudDuplicadaException();
        }
        BigDecimal acumuladoHoy = pagoRepository.sumarMontosDesde(cuenta.getId(), ESTATUS_VIGENTES,
                LocalDate.now(clock).atStartOfDay());
        if (acumuladoHoy.add(monto).compareTo(properties.getLimiteDiario()) > 0) {
            throw new LimiteDiarioExcedidoException();
        }
        if (tarjeta == null) {
            // Método anterior (app móvil): cargo al saldo de la cuenta
            if (cuenta.getSaldo().compareTo(monto) < 0) {
                throw new SaldoInsuficienteException();
            }
            cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        }

        PagoServicio pago = new PagoServicio();
        pago.setFolio(UUID.randomUUID().toString());
        pago.setIdempotencyKey(idempotencyKey);
        pago.setHuellaSolicitud(huella);
        pago.setClienteId(cliente.clienteId());
        pago.setCuenta(cuenta);
        pago.setIdServicio(solicitud.getIdServicio());
        pago.setIdProducto(solicitud.getIdProducto());
        pago.setNombreProducto(producto.nombre());
        pago.setNombreServicio(producto.servicio());
        pago.setReferencia(solicitud.getReferencia());
        pago.setMonto(monto);
        pago.setEstatus(EstatusPago.PENDIENTE);
        pago.setCreatedAt(ahora);
        if (tarjeta != null) {
            pago.setMetodoPago(METODO_TARJETA);
            pago.setTarjetaMarca(tarjeta.marca());
            pago.setTarjetaUltimos4(tarjeta.ultimos4());
            pago.setAutorizacionTarjeta(tarjeta.codigo());
        } else {
            pago.setMetodoPago(METODO_SALDO);
        }
        pagoRepository.save(pago);
        // La validación de la referencia se consume en la misma transacción: si otra petición la usó primero,
        // se revierte todo (sin cargo)
        if (validacionRepository.marcarUsada(solicitud.getValidacionId().toLowerCase(), pago.getFolio(), ahora) != 1) {
            throw new ValidacionYaUsadaException();
        }
        if (tarjeta == null) {
            movimientoRepository.save(new MovimientoCuenta(cuenta, pago, TipoMovimiento.CARGO, monto));
        }
        return new Reserva(pago, true);
    }

    /**
     * Paso 3: aplica el resultado del proveedor. Bloquea el pago para que la petición original, el job de
     * conciliación y un operador no apliquen resultados distintos al mismo tiempo; si ya es final, no hace nada.
     */
    @Transactional
    public PagoServicio aplicarResultado(Long pagoId, ResultadoProveedor resultado) {
        PagoServicio pago = pagoRepository.findByIdParaActualizar(pagoId).orElseThrow();
        if (pago.getEstatus().esFinal()) {
            return pago;
        }
        pago.setCodigoProveedor(resultado.codigo());
        switch (resultado.estado()) {
            case APROBADA -> {
                pago.setEstatus(EstatusPago.APROBADA);
                pago.setFolioProveedor(resultado.folioProveedor());
            }
            case RECHAZADA -> {
                devolverFondos(pago);
                pago.setEstatus(EstatusPago.RECHAZADA);
            }
            case INDETERMINADA -> {
                // El dinero se queda retenido: no se sabe si el proveedor aplicó el pago
                pago.setEstatus(EstatusPago.EN_CONCILIACION);
                log.warn("Pago enviado a conciliación: folio={} codigo={}", pago.getFolio(), resultado.codigo());
            }
        }
        return pagoRepository.save(pago);
    }

    /** Conciliación sin respuesta concluyente: suma un intento y, al llegar al máximo, pasa a revisión manual. */
    @Transactional
    public PagoServicio registrarIntentoSinResultado(Long pagoId) {
        PagoServicio pago = pagoRepository.findByIdParaActualizar(pagoId).orElseThrow();
        if (pago.getEstatus().esFinal() || pago.getEstatus() == EstatusPago.REVISION_MANUAL) {
            return pago;
        }
        pago.setIntentosConciliacion(pago.getIntentosConciliacion() + 1);
        if (pago.getIntentosConciliacion() >= properties.getConciliacion().getMaxIntentos()) {
            pago.setEstatus(EstatusPago.REVISION_MANUAL);
            log.error("Pago requiere revisión manual: folio={} intentos={}", pago.getFolio(), pago.getIntentosConciliacion());
        } else {
            pago.setEstatus(EstatusPago.EN_CONCILIACION);
        }
        return pagoRepository.save(pago);
    }

    private void devolverFondos(PagoServicio pago) {
        if (METODO_TARJETA.equals(pago.getMetodoPago())) {
            // Pagado con tarjeta: el reembolso va a la tarjeta, no al saldo. El estatus final evita repetirlo.
            procesadorTarjetas.reembolsar(new AutorizacionTarjeta(pago.getTarjetaMarca(), pago.getTarjetaUltimos4(),
                    pago.getAutorizacionTarjeta()), pago.getMonto());
            return;
        }
        // La restricción única (pago_id, tipo) es la última defensa contra una doble devolución
        if (movimientoRepository.existsByPagoIdAndTipo(pago.getId(), TipoMovimiento.REVERSO)) {
            return;
        }
        Cuenta cuenta = cuentaRepository.findByIdParaActualizar(pago.getCuenta().getId()).orElseThrow();
        cuenta.setSaldo(cuenta.getSaldo().add(pago.getMonto()));
        movimientoRepository.save(new MovimientoCuenta(cuenta, pago, TipoMovimiento.REVERSO, pago.getMonto()));
    }

    private void validarTitularYEstado(ClienteAutenticado cliente, Cuenta cuenta) {
        // Cuenta ajena e inexistente responden igual (403) para no revelar qué cuentas existen
        if (!cuenta.getCliente().getId().equals(cliente.clienteId())) {
            throw new OperacionNoAutorizadaException();
        }
        if (!Boolean.TRUE.equals(cuenta.getCliente().getActivo())) {
            throw new ClienteInactivoException();
        }
        if (!CUENTA_ACTIVA.equals(cuenta.getEstatus())) {
            throw new CuentaNoOperableException();
        }
    }

    /** @param nueva false si ya existía un pago con la misma llave de idempotencia */
    public record Reserva(PagoServicio pago, boolean nueva) {
    }
}
