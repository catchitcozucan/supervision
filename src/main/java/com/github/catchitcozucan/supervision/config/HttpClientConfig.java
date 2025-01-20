/**
 * Original work by Ola Aronsson 2020
 * Courtesy of nollettnoll AB &copy; 2012 - 2020
 * <p>
 * Licensed under the Creative Commons Attribution 4.0 International (the "License")
 * you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * https://creativecommons.org/licenses/by/4.0/
 * <p>
 * The software is provided “as is”, without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement. In no event shall the
 * authors or copyright holders be liable for any claim, damages or other liability,
 * whether in an action of contract, tort or otherwise, arising from, out of or
 * in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.config;

import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.entity.DeflateDecompressingEntity;
import org.apache.hc.client5.http.entity.GzipDecompressingEntity;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.brotli.dec.BrotliInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.catchitcozucan.supervision.utils.IOUtils.resourceToStream;

@Configuration
@Slf4j
public class HttpClientConfig {

    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private static final String GZIP = "gzip";
    private static final String DEFLATE = "deflate";
    private static final String BR = "br";
    private static final String COMMA_SPACE = ", ";
    private static final String SUPPORTED_COMPRESSION_VARIANTS = new StringBuilder(GZIP).append(COMMA_SPACE).append(DEFLATE).append(COMMA_SPACE).append(BR).toString();
    private static final String ACCEPT_ENCODING = "Accept-Encoding";

    private static final HttpRequestRetryStrategy NO_RETRY = new HttpRequestRetryStrategy() {

        @Override
        public boolean retryRequest(final HttpRequest httpRequest, final IOException e, final int i,
                                    final HttpContext httpContext) {
            return false;
        }

        @Override
        public boolean retryRequest(final HttpResponse httpResponse, final int i, final HttpContext httpContext) {
            return false;
        }

        @Override
        public TimeValue getRetryInterval(final HttpResponse httpResponse, final int i,
                                          final HttpContext httpContext) {
            return TimeValue.of(0l, TimeUnit.SECONDS);
        }
    };

    @Value("${jakarta.net.ssl.keyStore}")
    private Resource keyStore;
    @Value("${jakarta.net.ssl.keyStoreType}")
    private String keyStoreType;
    @Value("${jakarta.net.ssl.keyStorePassword}")
    private String keyStorePassword;

    @Value("${jakarta.net.ssl.trustStore}")
    private Resource trustStore;
    @Value("${jakarta.net.ssl.trustStoreType}")
    private String trustStoreType;
    @Value("${jakarta.net.ssl.trustStorePassword}")
    private String trustStorePassword;

    @Bean
    public SSLContext sslContext() {
        return loadSslContext();
    }

    @Bean
    public org.apache.hc.client5.http.classic.HttpClient httpClient() {

        final SSLConnectionSocketFactory sslsf =
                new SSLConnectionSocketFactory(loadSslContext(),
                        NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register(HTTPS, sslsf)
                        .register(HTTP, new PlainConnectionSocketFactory())
                        .build();

        final PoolingHttpClientConnectionManager cm =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);

        SocketConfig sc = SocketConfig.custom().setSoKeepAlive(true)
                .setSoReuseAddress(true)
                .setTcpNoDelay(true)
                .setSoTimeout(Timeout.of(5l, TimeUnit.SECONDS))
                .setSoLinger(Timeout.of(5l, TimeUnit.SECONDS)).build();
        cm.setDefaultSocketConfig(sc);

        return HttpClients.custom()
                .useSystemProperties()
                .setConnectionManager(cm)
                .setRetryStrategy(NO_RETRY)
                .addRequestInterceptorFirst((request, details, context) -> { // add accepting compressed headers _always_
                    if (!request.containsHeader(ACCEPT_ENCODING)) {
                        request.addHeader(ACCEPT_ENCODING, SUPPORTED_COMPRESSION_VARIANTS);
                    }
                }).addResponseInterceptorFirst((resp, details, context) -> {
                    ClassicHttpResponse response = (ClassicHttpResponse) resp;
                    HttpEntity entity = response.getEntity();
                    Supplier<List<? extends Header>> ceheader = entity != null ? entity.getTrailers() : null;
                    if (ceheader != null && ceheader.get() != null && ceheader.get().size() > 0) {
                        HeaderElement[] codecs = ceheader.get().toArray(new HeaderElement[0]);
                        for (int i = 0; i < codecs.length; i++) {
                            if (codecs[i].getName().equalsIgnoreCase(GZIP)) { // handling gzip
                                response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                            } else if (codecs[i].getName().equalsIgnoreCase(DEFLATE)) { // handling deflate
                                response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
                            } else if (codecs[i].getName().equalsIgnoreCase(BR)) { // handling brotli
                                try (BufferedReader br = new BufferedReader(new InputStreamReader(new BrotliInputStream(response.getEntity().getContent())))) {
                                    response.setEntity(new StringEntity(br.lines().collect(Collectors.joining())));
                                }
                            }
                        }
                    }
                })
                .build();
    }

    private SSLContext loadSslContext() {
        Optional<InputStream> inKey = resourceToStream(keyStore, "cert");
        Optional<InputStream> inTrust = resourceToStream(trustStore, "cert");
        KeyStore ks = null;
        KeyStore ts = null;
        try {
            if (inKey.isPresent()) {
                ks = KeyStore.getInstance(keyStoreType);
                ks.load(inKey.get(), keyStorePassword.toCharArray());
            }
            if (inTrust.isPresent()) {
                ts = KeyStore.getInstance(trustStoreType);
                ts.load(inTrust.get(), trustStorePassword.toCharArray());
            }
        } catch (Exception e) {
            log.error("failed to load ssl context", e);
        }

        if (ks != null && ts != null) {
            try {
                log.info("Loaded ssl context using keyStore={}, trustStore={}", keyStore, trustStore);
                final String alg = KeyManagerFactory.getDefaultAlgorithm();

                final KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(alg);
                kmFactory.init(ks, keyStorePassword.toCharArray());
                TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(alg);
                tmFactory.init(ts);

                SSLContext context = SSLContext.getInstance("SSL");
                context.init(kmFactory.getKeyManagers(), tmFactory.getTrustManagers(), new SecureRandom());
                return context;
            } catch (Exception e) {
                log.error("failed to load ssl context managers", e);
            } finally {
                if (inKey.isPresent()) {
                    IOUtils.closeQuietly(inKey.get());
                }
                if (inTrust.isPresent()) {
                    IOUtils.closeQuietly(inTrust.get());
                }
            }
        }
        throw new CatchitSupervisionRuntimeException("SSL context it NOT setup - this is problable bad");
    }

}
