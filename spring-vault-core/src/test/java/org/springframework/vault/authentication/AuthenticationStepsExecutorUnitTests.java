/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.vault.authentication;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.vault.VaultException;
import org.springframework.vault.client.VaultClients;
import org.springframework.vault.client.VaultClients.PrefixAwareUriTemplateHandler;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.vault.authentication.AuthenticationSteps.HttpRequestBuilder.get;
import static org.springframework.vault.authentication.AuthenticationSteps.HttpRequestBuilder.post;

/**
 * Unit tests for {@link AuthenticationStepsExecutor}.
 *
 * @author Mark Paluch
 */
public class AuthenticationStepsExecutorUnitTests {

	private RestTemplate restTemplate;
	private MockRestServiceServer mockRest;

	@Before
	public void before() {

		RestTemplate restTemplate = VaultClients.createRestTemplate();
		restTemplate.setUriTemplateHandler(new PrefixAwareUriTemplateHandler());

		this.mockRest = MockRestServiceServer.createServer(restTemplate);
		this.restTemplate = restTemplate;
	}

	@Test
	public void justTokenShouldLogin() {

		AuthenticationSteps steps = AuthenticationSteps.just(VaultToken.of("my-token"));

		assertThat(login(steps)).isEqualTo(VaultToken.of("my-token"));
	}

	@Test
	public void supplierOfStringShouldLoginWithMap() {

		AuthenticationSteps steps = AuthenticationSteps.fromSupplier(() -> "my-token")
				.login(VaultToken::of);

		assertThat(login(steps)).isEqualTo(VaultToken.of("my-token"));
	}

	@Test
	public void justLoginRequestShouldLogin() {

		mockRest.expect(requestTo("/auth/cert/login"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withSuccess()
								.contentType(MediaType.APPLICATION_JSON)
								.body("{"
										+ "\"auth\":{\"client_token\":\"my-token\", \"renewable\": true, \"lease_duration\": 10}"
										+ "}"));

		AuthenticationSteps steps = AuthenticationSteps.just(post("/auth/{path}/login",
				"cert").as(VaultResponse.class));

		assertThat(login(steps)).isEqualTo(VaultToken.of("my-token"));
	}

	@Test
	public void justLoginShouldFail() {

		mockRest.expect(requestTo("/auth/cert/login")).andExpect(method(HttpMethod.POST))
				.andRespond(withBadRequest().body("foo"));

		AuthenticationSteps steps = AuthenticationSteps.just(post("/auth/{path}/login",
				"cert").as(VaultResponse.class));

		assertThatExceptionOfType(VaultException.class)
				.isThrownBy(() -> login(steps))
				.withMessage(
						"HTTP request POST /auth/{path}/login AS class org.springframework.vault.support.VaultResponse "
								+ "in state null failed with Status 400 and body foo");
	}

	@Test
	public void initialRequestWithMapShouldLogin() {

		mockRest.expect(requestTo("somewhere/else")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess().contentType(MediaType.TEXT_PLAIN).body("foo"));

		mockRest.expect(requestTo("/auth/cert/login"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().string("foo-token"))
				.andRespond(
						withSuccess()
								.contentType(MediaType.APPLICATION_JSON)
								.body("{"
										+ "\"auth\":{\"client_token\":\"foo-token\", \"renewable\": true, \"lease_duration\": 10}"
										+ "}"));

		AuthenticationSteps steps = AuthenticationSteps
				.fromHttpRequest(get(URI.create("somewhere/else")).as(String.class))
				.onNext(System.out::println) //
				.map(s -> s.concat("-token")) //
				.login("/auth/cert/login");

		assertThat(login(steps)).isEqualTo(VaultToken.of("foo-token"));
	}

	@Test
	public void requestWithHeadersShouldLogin() {

		mockRest.expect(requestTo("somewhere/else")) //
				.andExpect(header("foo", "bar")) //
				.andExpect(method(HttpMethod.GET)) //
				.andRespond(withSuccess().contentType(MediaType.TEXT_PLAIN).body("foo"));

		mockRest.expect(requestTo("/auth/cert/login"))
				.andExpect(content().string("foo"))
				.andRespond(
						withSuccess()
								.contentType(MediaType.APPLICATION_JSON)
								.body("{"
										+ "\"auth\":{\"client_token\":\"foo-token\", \"renewable\": true, \"lease_duration\": 10}"
										+ "}"));

		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");

		AuthenticationSteps steps = AuthenticationSteps.fromHttpRequest(
				get(URI.create("somewhere/else")).with(headers).as(String.class)) //
				.login("/auth/cert/login");

		assertThat(login(steps)).isEqualTo(VaultToken.of("foo-token"));
	}

	private VaultToken login(AuthenticationSteps steps) {
		return new AuthenticationStepsExecutor(steps, restTemplate).login();
	}
}
