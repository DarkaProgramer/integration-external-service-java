package com.proyecto.servicios.client.simulado;

import com.proyecto.servicios.exception.pagos.TarjetaInvalidaException;
import com.proyecto.servicios.exception.pagos.TarjetaRechazadaException;
import com.proyecto.servicios.model.pagos.TarjetaDto;
import com.proyecto.servicios.service.pagos.AutorizacionTarjeta;
import com.proyecto.servicios.service.pagos.ProcesadorTarjetas;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Set;

/**
 * Procesador de tarjetas de práctica: valida como lo haría un adquirente real, pero no cobra dinero.
 * <ul>
 *     <li>Solo Visa y Mastercard de <b>débito</b>, pago de contado (una sola exhibición, sin meses).</li>
 *     <li>Número con dígito verificador válido (algoritmo de Luhn), vigencia y CVV de 3 dígitos.</li>
 *     <li>Las tarjetas de crédito se identifican por su BIN (primeros 6 dígitos) y se rechazan.</li>
 * </ul>
 * Tarjetas para probar rechazos del banco: terminación 0002 = fondos insuficientes, 0069 = tarjeta bloqueada.
 * Nunca se registra el número completo ni el CVV.
 */
@Slf4j
@Component
public class ProcesadorTarjetasSimulado implements ProcesadorTarjetas {

    /** BINs simulados de tarjetas de CRÉDITO (incluye las tarjetas de prueba más conocidas). */
    static final Set<String> BINS_CREDITO = Set.of("411111", "401288", "424242", "555555", "510510", "222300", "520082");
    private static final int ANIOS_MAXIMOS_VIGENCIA = 10;

    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public ProcesadorTarjetasSimulado(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AutorizacionTarjeta autorizar(TarjetaDto tarjeta, BigDecimal monto) {
        String numero = tarjeta.getNumero() == null ? "" : tarjeta.getNumero().replaceAll("\\s", "");
        String marca = marca(numero);
        if (marca == null) {
            throw new TarjetaInvalidaException("Solo se aceptan tarjetas de débito Visa o Mastercard");
        }
        if (numero.length() != 16 || !luhnValido(numero)) {
            throw new TarjetaInvalidaException("El número de tarjeta no es válido");
        }
        if (BINS_CREDITO.contains(numero.substring(0, 6))) {
            throw new TarjetaInvalidaException("Es una tarjeta de crédito: solo se aceptan tarjetas de débito (pago de contado)");
        }
        validarVigencia(tarjeta.getMesVencimiento(), tarjeta.getAnioVencimiento());
        if (tarjeta.getCvv() == null || !tarjeta.getCvv().matches("\\d{3}")) {
            throw new TarjetaInvalidaException("El CVV debe tener 3 dígitos");
        }

        String ultimos4 = numero.substring(12);
        switch (ultimos4) {
            case "0002" -> throw new TarjetaRechazadaException("El banco declinó el cargo: fondos insuficientes en la tarjeta. No se realizó ningún cargo");
            case "0069" -> throw new TarjetaRechazadaException("El banco declinó el cargo: la tarjeta está bloqueada. Comunícate con tu banco");
            default -> {
            }
        }
        String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        log.info("Cargo con tarjeta autorizado: marca={} ultimos4={} monto={} autorizacion={}", marca, ultimos4, monto, codigo);
        return new AutorizacionTarjeta(marca, ultimos4, codigo);
    }

    @Override
    public void reembolsar(AutorizacionTarjeta autorizacion, BigDecimal monto) {
        log.info("Reembolso a tarjeta: marca={} ultimos4={} monto={} autorizacion={}",
                autorizacion.marca(), autorizacion.ultimos4(), monto, autorizacion.codigo());
    }

    private void validarVigencia(Integer mes, Integer anio) {
        if (mes == null || anio == null || mes < 1 || mes > 12) {
            throw new TarjetaInvalidaException("La fecha de vencimiento no es válida");
        }
        YearMonth vence = YearMonth.of(anio < 100 ? 2000 + anio : anio, mes);
        YearMonth hoy = YearMonth.now(clock);
        if (vence.isBefore(hoy)) {
            throw new TarjetaInvalidaException("La tarjeta está vencida");
        }
        if (vence.isAfter(hoy.plusYears(ANIOS_MAXIMOS_VIGENCIA))) {
            throw new TarjetaInvalidaException("La fecha de vencimiento no es válida");
        }
    }

    /** VISA (4), MASTERCARD (51-55 y 2221-2720); cualquier otra red no se acepta. */
    static String marca(String numero) {
        if (numero.length() < 6 || !numero.chars().allMatch(Character::isDigit)) {
            return null;
        }
        if (numero.startsWith("4")) {
            return "VISA";
        }
        int dos = Integer.parseInt(numero.substring(0, 2));
        int cuatro = Integer.parseInt(numero.substring(0, 4));
        if ((dos >= 51 && dos <= 55) || (cuatro >= 2221 && cuatro <= 2720)) {
            return "MASTERCARD";
        }
        return null;
    }

    static boolean luhnValido(String numero) {
        int suma = 0;
        boolean doble = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (doble) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            suma += d;
            doble = !doble;
        }
        return suma % 10 == 0;
    }
}
