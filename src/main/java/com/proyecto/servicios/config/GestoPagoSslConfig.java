package com.proyecto.servicios.config;

import feign.Client;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Conexión TLS con Gestopago: certificado del proveedor en el truststore y versión de TLS fija.
 * <p>
 * Con {@code gestopago.ssl.enabled=true} todos los clientes Feign (token, catálogo, referencias y transacciones)
 * usan un {@link SSLContext} propio que confía en:
 * <ol>
 *     <li>los certificados normales de Java (cacerts), para no romper otras conexiones HTTPS;</li>
 *     <li>el truststore indicado en {@code gestopago.ssl.truststore} (JKS o PKCS12), si hay;</li>
 *     <li>los certificados PEM de {@code gestopago.ssl.certificado} (ruta {@code file:}/{@code classpath:}) y
 *     {@code gestopago.ssl.certificado-pem} (texto PEM pegado en una variable de entorno).</li>
 * </ol>
 * Y solo negocia las versiones de {@code gestopago.ssl.protocolos} (p. ej. {@code TLSv1.2}).
 * Si algo está mal configurado la aplicación no arranca, para que el error se vea en el deploy y no en un pago.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "gestopago.ssl.enabled", havingValue = "true")
public class GestoPagoSslConfig {

    @Bean
    public Client gestoPagoFeignClient(
            @Value("${gestopago.ssl.protocolos:TLSv1.2}") String protocolos,
            @Value("${gestopago.ssl.truststore:}") String truststore,
            @Value("${gestopago.ssl.truststore-password:changeit}") String truststorePassword,
            @Value("${gestopago.ssl.truststore-tipo:}") String truststoreTipo,
            @Value("${gestopago.ssl.certificado:}") String certificado,
            @Value("${gestopago.ssl.certificado-pem:}") String certificadoPem) throws GeneralSecurityException, IOException {
        String[] versiones = Arrays.stream(protocolos.split(",")).map(String::strip).filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        KeyStore confianza = construirTruststore(truststore, truststorePassword, truststoreTipo, certificado, certificadoPem);
        SSLContext contexto = SSLContext.getInstance(versiones.length == 1 ? versiones[0] : "TLS");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(confianza);
        contexto.init(null, tmf.getTrustManagers(), null);
        log.info("TLS de GestoPago activo: protocolos={} certificadosDeConfianza={}", String.join(",", versiones),
                confianza.size());
        return new Client.Default(new ProtocolosFijos(contexto.getSocketFactory(), versiones), null);
    }

    static KeyStore construirTruststore(String truststore, String password, String tipo, String certificado,
                                        String certificadoPem) throws GeneralSecurityException, IOException {
        KeyStore resultado = KeyStore.getInstance(KeyStore.getDefaultType());
        resultado.load(null, null);
        int i = 0;
        // 1. Certificados de Java (los que se usarían sin esta configuración)
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        for (var tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509) {
                for (X509Certificate c : x509.getAcceptedIssuers()) {
                    resultado.setCertificateEntry("java-" + i++, c);
                }
            }
        }
        // 2. Truststore propio (JKS/PKCS12) con el certificado de Gestopago importado con keytool
        if (StringUtils.isNotBlank(truststore)) {
            KeyStore propio = KeyStore.getInstance(StringUtils.isNotBlank(tipo) ? tipo.strip()
                    : truststore.toLowerCase().endsWith(".jks") ? "JKS" : "PKCS12");
            try (InputStream in = abrir(truststore)) {
                propio.load(in, password == null ? null : password.toCharArray());
            }
            for (String alias : java.util.Collections.list(propio.aliases())) {
                Certificate c = propio.getCertificate(alias);
                if (c != null) {
                    resultado.setCertificateEntry("truststore-" + alias, c);
                }
            }
        }
        // 3. Certificado(s) PEM/DER: archivo o texto en variable de entorno
        List<Certificate> extra = new ArrayList<>();
        if (StringUtils.isNotBlank(certificado)) {
            try (InputStream in = abrir(certificado)) {
                extra.addAll(leerCertificados(in));
            }
        }
        if (StringUtils.isNotBlank(certificadoPem)) {
            // Render a veces guarda los saltos de línea como "\n" literal
            String pem = certificadoPem.replace("\\n", "\n");
            extra.addAll(leerCertificados(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII))));
        }
        int n = 0;
        for (Certificate c : extra) {
            resultado.setCertificateEntry("gestopago-" + n++, c);
        }
        return resultado;
    }

    static Collection<? extends Certificate> leerCertificados(InputStream in) throws CertificateException {
        Collection<? extends Certificate> certificados = CertificateFactory.getInstance("X.509").generateCertificates(in);
        if (certificados.isEmpty()) {
            throw new CertificateException("El certificado de GestoPago está vacío o no es PEM/DER válido");
        }
        return certificados;
    }

    private static InputStream abrir(String ubicacion) throws IOException {
        String u = ubicacion.strip();
        Resource recurso = new DefaultResourceLoader().getResource(u.contains(":") && !u.matches("^[A-Za-z]:\\\\.*")
                ? u : "file:" + u);
        if (!recurso.exists()) {
            throw new IOException("No existe el archivo de certificado/truststore: " + u);
        }
        return recurso.getInputStream();
    }

    /** Fábrica de sockets que solo habilita las versiones de TLS configuradas. */
    static final class ProtocolosFijos extends SSLSocketFactory {
        private final SSLSocketFactory base;
        private final String[] protocolos;

        ProtocolosFijos(SSLSocketFactory base, String[] protocolos) {
            this.base = base;
            this.protocolos = protocolos;
        }

        private Socket ajustar(Socket s) {
            if (s instanceof SSLSocket ssl && protocolos.length > 0) {
                SSLParameters p = ssl.getSSLParameters();
                p.setProtocols(protocolos);
                ssl.setSSLParameters(p);
            }
            return s;
        }

        @Override public String[] getDefaultCipherSuites() { return base.getDefaultCipherSuites(); }
        @Override public String[] getSupportedCipherSuites() { return base.getSupportedCipherSuites(); }
        @Override public Socket createSocket() throws IOException { return ajustar(base.createSocket()); }
        @Override public Socket createSocket(Socket s, String h, int p, boolean a) throws IOException { return ajustar(base.createSocket(s, h, p, a)); }
        @Override public Socket createSocket(String h, int p) throws IOException { return ajustar(base.createSocket(h, p)); }
        @Override public Socket createSocket(String h, int p, InetAddress l, int lp) throws IOException { return ajustar(base.createSocket(h, p, l, lp)); }
        @Override public Socket createSocket(InetAddress h, int p) throws IOException { return ajustar(base.createSocket(h, p)); }
        @Override public Socket createSocket(InetAddress h, int p, InetAddress l, int lp) throws IOException { return ajustar(base.createSocket(h, p, l, lp)); }
    }
}
