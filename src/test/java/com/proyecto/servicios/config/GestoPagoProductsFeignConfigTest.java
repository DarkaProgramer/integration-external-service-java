package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestoPagoProductsFeignConfigTest {

    private final GestoPagoProductsFeignConfig config = new GestoPagoProductsFeignConfig();

    @Test
    void interceptor_conToken_agregaHeaderBearer() {
        RequestInterceptor interceptor = config.gestoPagoProductsAuthInterceptor("abc123", sinProveedor());
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer abc123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void interceptor_sinToken_lanzaErrorDeAutenticacion(String token) {
        RequestInterceptor interceptor = config.gestoPagoProductsAuthInterceptor(token, sinProveedor());

        assertThatThrownBy(() -> interceptor.apply(new RequestTemplate()))
                .isInstanceOfSatisfying(GestoPagoIntegrationException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(GestoPagoErrorType.AUTHENTICATION));
    }

    @Test
    void interceptor_conTokenDinamico_usaElTokenRenovado() {
        GestoPagoBearerToken proveedor = mock(GestoPagoBearerToken.class);
        when(proveedor.obtener(any())).thenReturn("dinamico");
        StaticListableBeanFactory fabrica = new StaticListableBeanFactory();
        fabrica.addBean("token", proveedor);
        RequestTemplate template = new RequestTemplate();

        config.gestoPagoProductsAuthInterceptor("fijo", fabrica.getBeanProvider(GestoPagoBearerToken.class)).apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer dinamico");
    }

    private static ObjectProvider<GestoPagoBearerToken> sinProveedor() {
        return new StaticListableBeanFactory().getBeanProvider(GestoPagoBearerToken.class);
    }
}
