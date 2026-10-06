package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoProductsFeignConfig;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "gestoPagoProducts",
        url = "${gestopago.products.url}",
        configuration = GestoPagoProductsFeignConfig.class)
public interface GestoPagoProductsClient {

    @GetMapping("/sistema/service/getProductList.do")
    GestoPagoProductListResponse getProductList();
}
