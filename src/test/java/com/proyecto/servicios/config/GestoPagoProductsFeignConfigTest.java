package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestoPagoProductsFeignConfigTest {

    private final GestoPagoProductsFeignConfig config = new GestoPagoProductsFeignConfig();

    @Test
    void interceptor_conToken_agregaHeaderBearer() {
        RequestInterceptor interceptor = config.gestoPagoProductsAuthInterceptor("abc123");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer abc123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void interceptor_sinToken_lanzaErrorDeAutenticacion(String token) {
        RequestInterceptor interceptor = config.gestoPagoProductsAuthInterceptor(token);

        assertThatThrownBy(() -> interceptor.apply(new RequestTemplate()))
                .isInstanceOfSatisfying(GestoPagoIntegrationException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(GestoPagoErrorType.AUTHENTICATION));
    }
}
