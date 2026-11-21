package com.proyecto.servicios.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLSocket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestoPagoSslConfigTest {

    private static String pem() throws Exception {
        return new String(new ClassPathResource("certs/prueba-gestopago.crt").getInputStream().readAllBytes(),
                StandardCharsets.US_ASCII);
    }

    private static boolean contieneCn(KeyStore ks) throws Exception {
        for (String alias : Collections.list(ks.aliases())) {
            if (((X509Certificate) ks.getCertificate(alias)).getSubjectX500Principal().getName()
                    .contains("prueba.gestopago.local")) {
                return true;
            }
        }
        return false;
    }

    @Test
    void certificadoDesdeClasspath_seAgregaJuntoAJava() throws Exception {
        KeyStore ks = GestoPagoSslConfig.construirTruststore("", "", "", "classpath:certs/prueba-gestopago.crt", "");

        assertThat(contieneCn(ks)).isTrue();
        assertThat(ks.size()).isGreaterThan(1);
    }

    @Test
    void certificadoPemEnVariable_conSaltosEscapados() throws Exception {
        String enUnaLinea = pem().replace("\n", "\\n");

        assertThat(contieneCn(GestoPagoSslConfig.construirTruststore("", "", "", "", enUnaLinea))).isTrue();
    }

    @Test
    void archivoInexistente_fallaAlArrancar() {
        assertThatThrownBy(() -> GestoPagoSslConfig.construirTruststore("", "", "", "file:/no/existe.crt", ""))
                .hasMessageContaining("No existe");
    }

    @Test
    void clienteSoloNegociaLaVersionConfigurada() throws Exception {
        var factory = new GestoPagoSslConfig.ProtocolosFijos(
                javax.net.ssl.SSLContext.getDefault().getSocketFactory(), new String[]{"TLSv1.2"});
        try (SSLSocket s = (SSLSocket) factory.createSocket()) {
            assertThat(s.getEnabledProtocols()).containsExactly("TLSv1.2");
        }
        assertThat(new GestoPagoSslConfig().gestoPagoFeignClient("TLSv1.2", "", "changeit", "",
                "classpath:certs/prueba-gestopago.crt", "")).isNotNull();
    }
}
