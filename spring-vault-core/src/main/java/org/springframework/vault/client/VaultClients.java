/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.vault.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;

/**
 * Vault Client factory to create {@link RestTemplate} configured to the needs of
 * accessing Vault.
 *
 * @author Mark Paluch
 * @see VaultEndpoint
 * @see RestTemplate
 */
public class VaultClients {

	/**
	 * Create a {@link RestTemplate} configured with {@link VaultEndpoint} and
	 * {@link ClientHttpRequestFactory}. The template accepts relative URIs without a
	 * leading slash that are expanded to use {@link VaultEndpoint}. {@link RestTemplate}
	 * is configured with a {@link ClientHttpRequestInterceptor} to enforce serialization
	 * to a byte array prior continuing the request. Eager serialization leads to a known
	 * request body size that is required to send a
	 * {@link org.springframework.http.HttpHeaders#CONTENT_LENGTH} request header.
	 * Otherwise, Vault will deny body processing.
	 *
	 * @param endpoint must not be {@literal null}.
	 * @param requestFactory must not be {@literal null}.
	 * @return the {@link RestTemplate}.
	 * @see org.springframework.http.client.Netty4ClientHttpRequestFactory
	 */
	public static RestTemplate createRestTemplate(VaultEndpoint endpoint,
			ClientHttpRequestFactory requestFactory) {

		RestTemplate restTemplate = new RestTemplate(requestFactory);

		restTemplate.setUriTemplateHandler(createUriTemplateHandler(endpoint));
		restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {

			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body,
					ClientHttpRequestExecution execution) throws IOException {
				return execution.execute(request, body);
			}
		});

		return restTemplate;
	}

	private static DefaultUriTemplateHandler createUriTemplateHandler(
			VaultEndpoint endpoint) {
		String baseUrl = String.format("%s://%s:%s/%s/", endpoint.getScheme(),
				endpoint.getHost(), endpoint.getPort(), "v1");

		DefaultUriTemplateHandler defaultUriTemplateHandler = new DefaultUriTemplateHandler();
		defaultUriTemplateHandler.setBaseUrl(baseUrl);
		return defaultUriTemplateHandler;
	}
}
