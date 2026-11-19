package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.entity.pagos.TarjetaGuardada;
import com.proyecto.servicios.exception.pagos.MedioPagoRequeridoException;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.TarjetaInvalidaException;
import com.proyecto.servicios.exception.pagos.TarjetaNoEncontradaException;
import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.TarjetaDto;
import com.proyecto.servicios.model.pagos.TarjetaGuardadaDto;
import com.proyecto.servicios.repositorys.pagos.TarjetaGuardadaRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.AutorizacionTarjeta;
import com.proyecto.servicios.service.pagos.ProcesadorTarjetas;
import com.proyecto.servicios.service.pagos.TarjetaTokenizada;
import com.proyecto.servicios.service.pagos.TarjetasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TarjetasServiceImpl implements TarjetasService {

    /** Límite razonable de tarjetas por cliente. */
    static final int MAXIMO_TARJETAS = 5;

    private final TarjetaGuardadaRepository repository;
    private final ProcesadorTarjetas procesador;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<TarjetaGuardadaDto> listar(ClienteAutenticado cliente) {
        exigirPermiso(cliente);
        return repository.findByClienteIdAndActivaTrueOrderByCreatedAtDesc(cliente.clienteId()).stream().map(this::aDto).toList();
    }

    @Override
    @Transactional
    public TarjetaGuardadaDto guardar(ClienteAutenticado cliente, TarjetaDto tarjeta) {
        exigirPermiso(cliente);
        return aDto(guardarTokenizada(cliente.clienteId(), procesador.tokenizar(tarjeta)));
    }

    @Override
    @Transactional
    public void eliminar(ClienteAutenticado cliente, String id) {
        exigirPermiso(cliente);
        TarjetaGuardada tarjeta = repository.findByTokenAndClienteIdAndActivaTrue(id, cliente.clienteId())
                .orElseThrow(TarjetaNoEncontradaException::new);
        tarjeta.setActiva(false);
        repository.save(tarjeta);
    }

    @Override
    @Transactional
    public AutorizacionTarjeta cobrar(ClienteAutenticado cliente, PagoServicioRequestDto solicitud) {
        if (solicitud.getTarjetaGuardadaId() != null && !solicitud.getTarjetaGuardadaId().isBlank()) {
            TarjetaGuardada guardada = repository.findByTokenAndClienteIdAndActivaTrue(solicitud.getTarjetaGuardadaId(), cliente.clienteId())
                    .orElseThrow(TarjetaNoEncontradaException::new);
            return procesador.autorizar(aTokenizada(guardada), solicitud.getCvv(), solicitud.getMonto());
        }
        TarjetaDto nueva = solicitud.getTarjeta();
        if (nueva == null) {
            throw new MedioPagoRequeridoException();
        }
        TarjetaTokenizada tokenizada = procesador.tokenizar(nueva);
        AutorizacionTarjeta autorizacion = procesador.autorizar(tokenizada, nueva.getCvv(), solicitud.getMonto());
        if (Boolean.TRUE.equals(solicitud.getGuardarTarjeta())) {
            try {
                guardarTokenizada(cliente.clienteId(), tokenizada);
            } catch (TarjetaInvalidaException e) {
                // Si ya tiene el máximo de tarjetas, el pago sigue; solo no se guarda
                log.info("Tarjeta no guardada: {}", e.getMessage());
            }
        }
        return autorizacion;
    }

    @Override
    public void reembolsar(AutorizacionTarjeta autorizacion, BigDecimal monto) {
        if (autorizacion != null) {
            procesador.reembolsar(autorizacion, monto);
        }
    }

    private TarjetaGuardada guardarTokenizada(Long clienteId, TarjetaTokenizada t) {
        List<TarjetaGuardada> actuales = repository.findByClienteIdAndActivaTrueOrderByCreatedAtDesc(clienteId);
        // Misma tarjeta ya guardada (marca, terminación y vencimiento): no se duplica
        for (TarjetaGuardada g : actuales) {
            if (g.getMarca().equals(t.marca()) && g.getUltimos4().equals(t.ultimos4())
                    && g.getMesVencimiento() == t.mesVencimiento() && g.getAnioVencimiento() == t.anioVencimiento()) {
                return g;
            }
        }
        if (actuales.size() >= MAXIMO_TARJETAS) {
            throw new TarjetaInvalidaException("Solo puedes guardar hasta " + MAXIMO_TARJETAS + " tarjetas. Elimina una en Mi cuenta");
        }
        TarjetaGuardada nueva = new TarjetaGuardada();
        nueva.setClienteId(clienteId);
        nueva.setToken(t.token());
        nueva.setMarca(t.marca());
        nueva.setUltimos4(t.ultimos4());
        nueva.setMesVencimiento(t.mesVencimiento());
        nueva.setAnioVencimiento(t.anioVencimiento());
        nueva.setTitular(t.titular());
        nueva.setActiva(true);
        nueva.setCreatedAt(LocalDateTime.now(clock));
        return repository.save(nueva);
    }

    private static TarjetaTokenizada aTokenizada(TarjetaGuardada g) {
        return new TarjetaTokenizada(g.getToken(), g.getMarca(), g.getUltimos4(), g.getMesVencimiento(), g.getAnioVencimiento(), g.getTitular());
    }

    private TarjetaGuardadaDto aDto(TarjetaGuardada g) {
        return TarjetaGuardadaDto.builder()
                .id(g.getToken())
                .marca(g.getMarca())
                .ultimos4(g.getUltimos4())
                .vencimiento(String.format("%02d/%02d", g.getMesVencimiento(), g.getAnioVencimiento() % 100))
                .titular(g.getTitular())
                .vencida(YearMonth.of(g.getAnioVencimiento(), g.getMesVencimiento()).isBefore(YearMonth.now(clock)))
                .build();
    }

    private static void exigirPermiso(ClienteAutenticado cliente) {
        if (!cliente.tienePermiso(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS)) {
            throw new OperacionNoAutorizadaException();
        }
    }
}
