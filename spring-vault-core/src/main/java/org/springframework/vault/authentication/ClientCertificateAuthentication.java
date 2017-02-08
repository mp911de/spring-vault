/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.vault.authentication;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.vault.client.PreviousVaultClient;
import org.springframework.vault.client.VaultException;
import org.springframework.vault.client.VaultResponseEntity;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;

/**
 * TLS Client Certificate {@link ClientAuthentication}.
 *
 * @author Mark Paluch
 */
public class ClientCertificateAuthentication implements ClientAuthentication {

	private final static Log logger = LogFactory
			.getLog(ClientCertificateAuthentication.class);

	private final PreviousVaultClient vaultClient;

	/**
	 * Creates a {@link ClientCertificateAuthentication} using {@link PreviousVaultClient}
	 * .
	 *
	 * @param vaultClient must not be {@literal null}.
	 */
	public ClientCertificateAuthentication(PreviousVaultClient vaultClient) {

		Assert.notNull(vaultClient, "VaultClient must not be null");

		this.vaultClient = vaultClient;
	}

	@Override
	public VaultToken login() {
		return createTokenUsingTlsCertAuthentication("cert");
	}

	private VaultToken createTokenUsingTlsCertAuthentication(String path) {

		VaultResponseEntity<VaultResponse> entity = vaultClient.postForEntity(
				String.format("auth/%s/login", path), Collections.emptyMap(),
				VaultResponse.class);

		if (!entity.isSuccessful()) {
			throw new VaultException(String.format(
					"Cannot login using TLS certificates: %s", entity.getMessage()));
		}

		logger.debug("Login successful using TLS certificates");

		return LoginTokenUtil.from(entity.getBody().getAuth());
	}
}
