/*
 * Copyright 2018 the original author or authors.
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.RestOperationsCallback;
import org.springframework.vault.core.VaultTokenOperations;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.support.VaultTokenRequest;
import org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration;
import org.springframework.vault.util.IntegrationTestSupport;
import org.springframework.vault.util.TestWebClientFactory;
import org.springframework.vault.util.Version;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;
import static org.springframework.vault.util.Settings.createSslConfiguration;
import static org.springframework.vault.util.Settings.findWorkDir;

/**
 * Integration tests for {@link ReactiveLifecycleAwareSessionManager}.
 *
 * @author Mark Paluch
 */
public class ReactiveLifecycleAwareSessionManagerIntegrationTests extends
		IntegrationTestSupport {

	private ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

	@Before
	public void before() {

		assumeTrue(prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.6.2")));

		taskScheduler.afterPropertiesSet();

		if (!prepare().hasAuth("cert")) {
			prepare().mountAuth("cert");
		}

		prepare().getVaultOperations().doWithSession(
				(RestOperationsCallback<Object>) restOperations -> {
					File workDir = findWorkDir();

					String certificate = Files.contentOf(new File(workDir,
							"ca/certs/client.cert.pem"), StandardCharsets.US_ASCII);

					Map<String, Object> body = new HashMap<>();
					body.put("certificate", certificate);
					body.put("ttl", 2 /* seconds */);
					body.put("max_ttl", 2 /* seconds */);

					return restOperations.postForEntity("auth/cert/certs/my-role", body,
							Map.class);
				});
	}

	@After
	public void tearDown() {
		taskScheduler.destroy();
	}

	@Test
	public void shouldLogin() {

		LoginToken loginToken = createLoginToken();

		ReactiveLifecycleAwareSessionManager sessionManager = new ReactiveLifecycleAwareSessionManager(
				() -> Mono.just(loginToken), taskScheduler, prepare().getWebClient());

		sessionManager.getVaultToken().as(StepVerifier::create).expectNext(loginToken)
				.verifyComplete();
	}

	// Expect no exception to be thrown.
	@Test
	public void shouldRenewToken() {

		VaultTokenOperations tokenOperations = prepare().getVaultOperations()
				.opsForToken();

		VaultTokenRequest tokenRequest = VaultTokenRequest.builder() //
				.renewable().ttl(1, TimeUnit.HOURS) //
				.explicitMaxTtl(10, TimeUnit.HOURS) //
				.build();

		VaultToken token = tokenOperations.create(tokenRequest).getToken();

		LoginToken loginToken = LoginToken.renewable(token, Duration.ZERO);

		final AtomicInteger counter = new AtomicInteger();
		ReactiveLifecycleAwareSessionManager sessionManager = new ReactiveLifecycleAwareSessionManager(
				() -> Flux.fromStream(Stream.of((VaultToken) loginToken)).next(),
				taskScheduler, prepare().getWebClient()) {

			@Override
			public Mono<VaultToken> getVaultToken() throws VaultException {

				if (counter.getAndIncrement() > 0) {
					throw new IllegalStateException();
				}

				return super.getVaultToken();
			}
		};

		sessionManager.getSessionToken().as(StepVerifier::create).expectNext(loginToken)
				.verifyComplete();
		sessionManager.renewToken().as(StepVerifier::create).expectNext(loginToken)
				.verifyComplete();
	}

	@Test
	@Ignore("Run me manually, I take some seconds to complete")
	public void shouldRenewTokenAfterExpiry() throws InterruptedException {

		SslConfiguration sslConfiguration = prepareCertAuthenticationMethod();

		WebClient webClient = TestWebClientFactory.create(sslConfiguration);
		final AtomicInteger getTokenCounter = new AtomicInteger();
		AuthenticationStepsOperator stepsOperator = new AuthenticationStepsOperator(
				ClientCertificateAuthentication.createAuthenticationSteps(), webClient) {
			@Override
			public Mono<VaultToken> getVaultToken() throws VaultException {
				return super.getVaultToken().doAfterTerminate(
						getTokenCounter::incrementAndGet);
			}
		};

		ReactiveLifecycleAwareSessionManager sessionManager = new ReactiveLifecycleAwareSessionManager(
				stepsOperator, taskScheduler, prepare().getWebClient());

		VaultToken firstToken = sessionManager.getSessionToken().block();
		VaultToken cached = sessionManager.getSessionToken().block();

		TimeUnit.SECONDS.sleep(5);
		VaultToken nextToken = sessionManager.getSessionToken().block();

		assertThat(firstToken).isNotNull().isEqualTo(cached);
		assertThat(nextToken).isNotNull();
		assertThat(nextToken.getToken()).isNotEqualTo(firstToken.toString());
		assertThat(nextToken.getToken()).isNotEqualTo(firstToken.toString());
		assertThat(getTokenCounter.get()).isEqualTo(2);
	}

	@Test
	public void shouldRevokeOnDisposal() {

		final LoginToken loginToken = createLoginToken();

		ReactiveLifecycleAwareSessionManager sessionManager = new ReactiveLifecycleAwareSessionManager(
				() -> Flux.fromStream(Stream.of((VaultToken) loginToken)).next(),
				taskScheduler, prepare().getWebClient());

		sessionManager.getSessionToken().as(StepVerifier::create).expectNext(loginToken)
				.verifyComplete();
		sessionManager.destroy();

		prepare().getVaultOperations().doWithSession(
				restOperations -> {

					try {
						restOperations.getForEntity("auth/token/lookup/{token}",
								Map.class, loginToken.toCharArray());
						fail("Missing HttpStatusCodeException");
					}
					catch (HttpStatusCodeException e) {
						// Compatibility across Vault versions.
						assertThat(e.getStatusCode()).isIn(HttpStatus.BAD_REQUEST,
								HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
					}

					return null;
				});
	}

	private LoginToken createLoginToken() {

		VaultTokenOperations tokenOperations = prepare().getVaultOperations()
				.opsForToken();
		VaultToken token = tokenOperations.createOrphan().getToken();

		return LoginToken.of(token.getToken());
	}

	static SslConfiguration prepareCertAuthenticationMethod() {

		SslConfiguration original = createSslConfiguration();

		return new SslConfiguration(KeyStoreConfiguration.of(new FileSystemResource(
				new File(findWorkDir(), "client-cert.jks")), "changeit".toCharArray()),
				original.getTrustStoreConfiguration());
	}
}
