/*
 * Copyright 2017-2019 the original author or authors.
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.vault.authentication.AppRoleAuthenticationOptions.RoleId;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.SecretId;
import org.springframework.vault.support.VaultToken;

/**
 * Predefined {@link RoleId} token types.
 *
 * @author Mark Paluch
 * @since 2.0
 */
class AppRoleTokens {

	/**
	 * Absent secretId.
	 */
	enum AbsentSecretId implements SecretId {
		INSTANCE;
	}

	/**
	 * Wrapped roleId/secretId via Cubbyhole.
	 */
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	@Getter
	static class Wrapped implements RoleId, SecretId {

		final VaultToken initialToken;
	}

	/**
	 * Pull-mode.
	 */
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	@Getter
	static class Pull implements RoleId, SecretId {

		final VaultToken initialToken;
	}

	/**
	 * Static, provided roleId/secretId.
	 */
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	@Getter
	static class Provided implements RoleId, SecretId {

		final String value;
	}
}
