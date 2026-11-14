package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.model.catalogo.ProductoCatalogoDto;
import com.proyecto.servicios.model.comun.ApiResponse;
import com.proyecto.servicios.seguridad.ClienteActual;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de servicios pagables, consultado en vivo a Gestopago (con una copia en memoria de
 * {@code pagos.catalogo.ttl-minutos}). Si Gestopago no responde se devuelve 503: nunca datos fijos.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/catalogo", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogoController {

    private final CatalogoProductosService catalogoProductosService;
    private final PagosProperties pagosProperties;

    @GetMapping("/productos")
    public ResponseEntity<ApiResponse<List<ProductoCatalogoDto>>> productos(@ClienteActual ClienteAutenticado cliente) {
        List<ProductoCatalogoDto> productos = catalogoProductosService.listar().stream().map(this::aDto).toList();
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Catálogo de productos", productos));
    }

    private ProductoCatalogoDto aDto(ProductoCatalogo p) {
        boolean variable = !p.tienePrecioFijo();
        return ProductoCatalogoDto.builder()
                .identificador(p.identificador())
                .idServicio(p.idServicio())
                .idProducto(p.idProducto())
                .nombre(p.nombre())
                .servicio(p.servicio())
                .descripcion(p.descripcion())
                .categoria(p.categoria())
                .logoUrl(p.logoUrl())
                .precio(variable ? null : p.precio())
                .montoVariable(variable)
                .montoMinimo(variable ? pagosProperties.getMontoMinimo() : null)
                .montoMaximo(variable ? pagosProperties.getMontoMaximo() : null)
                .referencia(ProductoCatalogoDto.ReferenciaDto.builder()
                        .longitudMinima(p.referencia().longitudMinima())
                        .longitudMaxima(p.referencia().longitudMaxima())
                        .soloDigitos(p.referencia().soloDigitos())
                        .build())
                .build();
    }
}
