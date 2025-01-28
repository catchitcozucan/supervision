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
package com.github.catchitcozucan.supervision.service.http;

import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Map;

import static com.github.catchitcozucan.supervision.exception.ErrorCodes.*;

@Component
@RequiredArgsConstructor
public class SimpleHttpGetter {
	private static final String PROXY_IS_AN_UNKNOWN_HOST = "Proxy is an unknown host";
	private static final String COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S = "could not exeecute GET towards url %s";

	private static final String COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_EXC_NON_200 = "could not exeecute GET towards url %s. Server responded HTTP-%d with message %s";
	private static final String COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_EXC = "could not exeecute GET towards url %s. Exception : %s";
	private static final String COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_NO_CT = "could not exeecute GET towards url %s - got no Content-Type back!";
	private static final String COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_BAD_CT = "could not exeecute GET towards url %s - got wrong Content-Type back : %s. Expecting application/json; charset=utf-8";
	private static final String JSON = "json";


	private static final RequestConfig DEFAULT_CONNECTION_CONFIG = RequestConfig.custom().setConnectTimeout(Timeout.ofSeconds(3l)) //  the time to establish the connection with the remote host
			.setConnectionRequestTimeout(Timeout.ofSeconds(5l)) // timeout used when requesting a connection from the local cm
			.build(); // the time waiting for data – after establishing the connection; maximum time of inactivity between two data packets
	private static final String CONTENT_TYPE = "content-type";
	private static final String JSON_UTF8 = "application/json; charset=utf-8";

	private final HttpClient httpClient;

	public ResponseCodeAndBody executeGet(URI uri, Map<String, String> headers, String proxyHost, Integer proxyPort) {
		HttpUriRequestBase req = new HttpGet(uri);
		req.addHeader(CONTENT_TYPE, JSON_UTF8);
		req.addHeader("Accept", "application/json, " + JSON_UTF8);
		if (headers != null && !headers.isEmpty()) {
			headers.entrySet().stream().forEach(entry -> {
				req.addHeader(entry.getKey(), entry.getValue());
			});
		}

		Integer actualProxyPort = null;
		if (proxyHost != null && proxyHost.length() > 1) {
			if (proxyPort == null) {
				actualProxyPort = 80;
			} else {
				actualProxyPort = proxyPort;
			}
			RequestConfig.Builder requestConfigNew = toNewBuilderWithOriginalTimeoutSettings(req);
			if (actualProxyPort != null) {
				try {
					requestConfigNew.setProxy(new HttpHost(InetAddress.getByName(proxyHost), actualProxyPort));
				} catch (UnknownHostException e) {
					throw new CatchitSupervisionRuntimeException(PROXY_IS_AN_UNKNOWN_HOST, e, PROXY_IS_UNKNOWN);
				}
			}
			req.setConfig(requestConfigNew.build());
		}

		try {
			HttpResponse resp = httpClient.execute(req);
			int responseCode = resp.getCode();
			if (responseCode < 200 || responseCode >= 300) {
				throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_EXC_NON_200, uri.toString(), responseCode, resp.getReasonPhrase(), BAD_RESPONSE_HTTP_CODE), BAD_RESPONSE_HTTP_CODE);
			}
			HttpEntity entity = ((ClassicHttpResponse) resp).getEntity();
			String contentTypeValue = entity.getContentType();
			if (!StringUtils.hasContents(contentTypeValue)) {
				throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_NO_CT, uri.toString()), NO_CONTENT_TYPE_IN_RESPONSE);
			} else if (!contentTypeValue.toLowerCase().contains(JSON)) {
				throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_BAD_CT, uri.toString(), contentTypeValue), BAD_CONTENT_TYPE_IN_RESPONSE);
			}
			return ResponseCodeAndBody.builder().body(StringUtils.fromStreamCloseUponFinish(entity.getContent())).responseCode(responseCode).build();
		} catch (IOException e) {
			if (e instanceof HttpHostConnectException) {
				throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_EXC, uri.toString(), e.getMessage()), e, REQUEST_GIVES_CONNECTION_REFUSED);
			}
			if (e instanceof UnknownHostException) {
				throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_EXC, uri.toString(), e.getMessage()), e, REQUEST_GIVES_UNKNOWN_HOST_EXCEPTION);
			} else if (e instanceof HttpTimeoutException) {
				throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_EXC, uri.toString(), e.getMessage()), e, REQUEST_GIVES_HTTP_TIMEOUT_EXCEPTION);
			} else if (e instanceof HttpConnectTimeoutException || e instanceof ConnectTimeoutException) {
				throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S_EXC, uri.toString(), e.getMessage()), e, REQUEST_GIVES_HTTP_CONECTION_TIMEOUT_EXCEPTION);
			}
			throw new CatchitSupervisionRuntimeException(String.format(COULD_NOT_EXEECUTE_GET_TOWARDS_URL_S, uri.toString()), e);
		}
	}

	private static RequestConfig.Builder toNewBuilderWithOriginalTimeoutSettings(HttpUriRequestBase req) {
		return RequestConfig.custom().setConnectionRequestTimeout(DEFAULT_CONNECTION_CONFIG.getConnectionRequestTimeout()).setConnectTimeout(DEFAULT_CONNECTION_CONFIG.getConnectTimeout());
	}


}
