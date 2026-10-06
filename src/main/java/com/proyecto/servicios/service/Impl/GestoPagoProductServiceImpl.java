package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductsClient;
import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.GestoPagoProductService;
import feign.FeignException;
import feign.RetryableException;
import feign.codec.DecodeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    private final GestoPagoProductsClient productsClient;

    public GestoPagoProductServiceImpl(GestoPagoProductsClient productsClient) {
        this.productsClient = productsClient;
    }

    @Override
    public GestoPagoProductListResponse obtenerProductos() {
        log.info("Inicio de invocacion a GestoPago getProductList");
        long inicio = System.nanoTime();
        try {
            GestoPagoProductListResponse response = consultarProductos();
            log.info("Fin de invocacion a GestoPago getProductList: resultado=OK, productos={}, duracionMs={}",
                    totalProductos(response), duracionMs(inicio));
            return response;
        } catch (GestoPagoIntegrationException e) {
            // Solo se registra tipo y causa (clase): nunca mensajes, URLs, headers ni el token
            log.error("Fin de invocacion a GestoPago getProductList: resultado=ERROR, tipo={}, causa={}, duracionMs={}",
                    e.getErrorType(), causaResumida(e), duracionMs(inicio));
            throw e;
        }
    }

    private GestoPagoProductListResponse consultarProductos() {
        try {
            GestoPagoProductListResponse response = productsClient.getProductList();
            if (response == null) {
                throw new GestoPagoIntegrationException(GestoPagoErrorType.INVALID_RESPONSE);
            }
            return response;
        } catch (FeignException e) {
            throw traducir(e);
        }
    }

    private GestoPagoIntegrationException traducir(FeignException e) {
        if (e instanceof RetryableException) {
            return new GestoPagoIntegrationException(esTimeout(e)
                    ? GestoPagoErrorType.TIMEOUT
                    : GestoPagoErrorType.COMMUNICATION, e);
        }
        if (e instanceof DecodeException) {
            return new GestoPagoIntegrationException(GestoPagoErrorType.INVALID_RESPONSE, e);
        }
        if (e.status() == 401 || e.status() == 403) {
            return new GestoPagoIntegrationException(GestoPagoErrorType.AUTHENTICATION, e);
        }
        return new GestoPagoIntegrationException(GestoPagoErrorType.UNSUCCESSFUL_RESPONSE, e);
    }

    private boolean esTimeout(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof InterruptedIOException || actual instanceof HttpTimeoutException) {
                return true;
            }
            if (actual.getCause() == actual) {
                break;
            }
        }
        return false;
    }

    private int totalProductos(GestoPagoProductListResponse response) {
        return response.getProductList() == null ? 0 : response.getProductList().size();
    }

    private long duracionMs(long inicioNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
    }

    private String causaResumida(GestoPagoIntegrationException e) {
        return e.getCause() == null ? "N/A" : e.getCause().getClass().getSimpleName();
    }
}
