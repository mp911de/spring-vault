/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.vault.authentication.AppRoleTokens.AbsentSecretId;
import org.springframework.vault.authentication.AppRoleTokens.Provided;
import org.springframework.vault.authentication.AppRoleTokens.Pull;
import org.springframework.vault.support.VaultToken;

/**
 * Authentication options for {@link AppRoleAuthentication}.
 * <p>
 * Authentication options provide the path, roleId and pull/push mode.
 * {@link AppRoleAuthentication} can be constructed using {@link #builder()}. Instances of
 * this class are immutable once constructed.
 *
 * @author Mark Paluch
 * @author Vincent Le Nair
 * @author Christophe Tafani-Dereeper
 * @see AppRoleAuthentication
 * @see #builder()
 */
public class AppRoleAuthenticationOptions {

	public static final String DEFAULT_APPROLE_AUTHENTICATION_PATH = "approle";

	/**
	 * Path of the approle authentication backend mount.
	 */
	private final String path;

	/**
	 * The RoleId.
	 */
	private final RoleId roleId;

	/**
	 * The Bind SecretId.
	 */
	private final SecretId secretId;

	/**
	 * Role name used to get roleId and secretID
	 */
	@Nullable
	private final String appRole;

	/**
	 * Token associated for pull mode (retrieval of secretId/roleId).
	 * @deprecated since 2.0, use {@link RoleId#pull(VaultToken)}/
	 * {@link SecretId#pull(VaultToken)} to configure pull mode for roleId/secretId.
	 */
	@Nullable
	@Deprecated
	private final VaultToken initialToken;

	private AppRoleAuthenticationOptions(String path, RoleId roleId, SecretId secretId,
			@Nullable String appRole, @Nullable VaultToken initialToken) {

		this.path = path;
		this.roleId = roleId;
		this.secretId = secretId;
		this.appRole = appRole;
		this.initialToken = initialToken;
	}

	/**
	 * @return a new {@link AppRoleAuthenticationOptionsBuilder}.
	 */
	public static AppRoleAuthenticationOptionsBuilder builder() {
		return new AppRoleAuthenticationOptionsBuilder();
	}

	/**
	 * @return the mount path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the RoleId.
	 */
	public RoleId getRoleId() {
		return roleId;
	}

	/**
	 * @return the bound SecretId.
	 */
	public SecretId getSecretId() {
		return secretId;
	}

	/**
	 * @return the bound AppRole.
	 * @since 1.1
	 */
	@Nullable
	public String getAppRole() {
		return appRole;
	}

	/**
	 * @return the initial token for roleId/secretId retrieval in pull mode.
	 * @since 1.1
	 * @deprecated since 2.0, use {@link #getRoleId()}/{@link #getSecretId()} to obtain
	 * configuration modes (pull/wrapped) for an AppRole token.
	 */
	@Nullable
	@Deprecated
	public VaultToken getInitialToken() {
		return initialToken;
	}

	/**
	 * Builder for {@link AppRoleAuthenticationOptions}.
	 */
	public static class AppRoleAuthenticationOptionsBuilder {

		private String path = DEFAULT_APPROLE_AUTHENTICATION_PATH;

		@Nullable
		private String providedRoleId;

		@Nullable
		private RoleId roleId;

		@Nullable
		private String providedSecretId;

		@Nullable
		private SecretId secretId;

		@Nullable
		private String appRole;

		@Nullable
		@Deprecated
		private VaultToken initialToken;

		AppRoleAuthenticationOptionsBuilder() {
		}

		/**
		 * Configure the mount path.
		 *
		 * @param path must not be empty or {@literal null}.
		 * @return {@code this} {@link AppRoleAuthenticationOptionsBuilder}.
		 * @see #DEFAULT_APPROLE_AUTHENTICATION_PATH
		 */
		public AppRoleAuthenticationOptionsBuilder path(String path) {

			Assert.hasText(path, "Path must not be empty");

			this.path = path;
			return this;
		}

		/**
		 * Configure the RoleId.
		 *
		 * @param roleId must not be empty or {@literal null}.
		 * @return {@code this} {@link AppRoleAuthenticationOptionsBuilder}.
		 * @since 2.0
		 */
		public AppRoleAuthenticationOptionsBuilder roleId(RoleId roleId) {

			Assert.notNull(roleId, "RoleId must not be null");

			this.roleId = roleId;
			return this;
		}

		/**
		 * Configure the RoleId.
		 *
		 * @param roleId must not be empty or {@literal null}.
		 * @return {@code this} {@link AppRoleAuthenticationOptionsBuilder}.
		 * @deprecated since 2.0, use {@link #roleId(RoleId)}.
		 */
		@Deprecated
		public AppRoleAuthenticationOptionsBuilder roleId(String roleId) {

			Assert.hasText(roleId, "RoleId must not be empty");

			this.providedRoleId = roleId;
			return this;
		}

		/**
		 * Configure a {@code secretId}.
		 *
		 * @param secretId must not be empty or {@literal null}.
		 * @return {@code this} {@link AppRoleAuthenticationOptionsBuilder}.
		 * @since 2.0
		 */
		public AppRoleAuthenticationOptionsBuilder secretId(SecretId secretId) {

			Assert.notNull(secretId, "SecretId must not be null");

			this.secretId = secretId;
			return this;
		}

		/**
		 * Configure a {@code secretId}.
		 *
		 * @param secretId must not be empty or {@literal null}.
		 * @return {@code this} {@link AppRoleAuthenticationOptionsBuilder}.
		 * @deprecated since 2.0, use {@link #secretId(SecretId)}.
		 */
		@Deprecated
		public AppRoleAuthenticationOptionsBuilder secretId(String secretId) {

			Assert.hasText(secretId, "SecretId must not be empty");

			this.providedSecretId = secretId;
			return this;
		}

		/**
		 * Configure a {@code appRole}.
		 *
		 * @param appRole must not be empty or {@literal null}.
		 * @return {@code this} {@link AppRoleAuthenticationOptionsBuilder}.
		 * @since 1.1
		 */
		public AppRoleAuthenticationOptionsBuilder appRole(String appRole) {

			Assert.hasText(appRole, "AppRole must not be empty");

			this.appRole = appRole;
			return this;
		}

		/**
		 * Configure a {@code initialToken}.
		 *
		 * @param initialToken must not be empty or {@literal null}.
		 * @return {@code this} {@link AppRoleAuthenticationOptionsBuilder}.
		 * @since 1.1
		 * @deprecated since 2.0, use {@link #roleId(RoleId)}/{@link #secretId(SecretId)}
		 * to configure pull mode.
		 */
		@Deprecated
		public AppRoleAuthenticationOptionsBuilder initialToken(VaultToken initialToken) {

			Assert.notNull(initialToken, "InitialToken must not be null");

			this.initialToken = initialToken;
			return this;
		}

		/**
		 * Build a new {@link AppRoleAuthenticationOptions} instance. Requires
		 * {@link #roleId(String)} for push mode or {@link #appRole(String)} and
		 * {@link #initialToken(VaultToken)} for pull mode to be configured.
		 *
		 * @return a new {@link AppRoleAuthenticationOptions}.
		 */
		public AppRoleAuthenticationOptions build() {

			Assert.hasText(path, "Path must not be empty");

			if (secretId == null) {

				if (providedSecretId != null) {
					secretId(SecretId.provided(providedSecretId));
				}
				else if (initialToken != null) {
					secretId(SecretId.pull(initialToken));
				}
				else {
					secretId(SecretId.absent());
				}
			}

			if (roleId == null) {

				if (providedRoleId != null) {
					roleId(RoleId.provided(providedRoleId));
				}
				else {

					Assert.notNull(
							initialToken,
							"AppRole authentication configured for pull mode. InitialToken must not be null (pull mode)");
					roleId(RoleId.pull(initialToken));
				}
			}

			if (roleId instanceof Pull || secretId instanceof Pull) {
				Assert.notNull(appRole,
						"AppRole authentication configured for pull mode. AppRole must not be null.");
			}

			return new AppRoleAuthenticationOptions(path, roleId, secretId, appRole,
					initialToken);
		}
	}

	/**
	 * RoleId type encapsulating how the roleId is actually obtained. Provides factory
	 * methods to obtain a {@link RoleId} by wrapping, pull-mode or whether to use a
	 * string literal.
	 *
	 * @since 2.0
	 */
	public interface RoleId {

		/**
		 * Create a {@link RoleId} object that obtains its value from unwrapping a
		 * response using the {@link VaultToken initial token} from a Cubbyhole.
		 *
		 * @param initialToken must not be {@literal null}.
		 * @return {@link RoleId} object that obtains its value from unwrapping a response
		 * using the {@link VaultToken initial token}.
		 * @see org.springframework.vault.client.VaultResponses#unwrap(String, Class)
		 */
		static RoleId wrapped(VaultToken initialToken) {

			Assert.notNull(initialToken, "Initial token must not be null");

			return new AppRoleTokens.Wrapped(initialToken);
		}

		/**
		 * Create a {@link RoleId} that obtains its value using pull-mode, specifying a
		 * {@link VaultToken initial token}. The token policy must allow reading the
		 * roleId from {@code auth/approle/role/(role-name)/role-id}.
		 *
		 * @param initialToken must not be {@literal null}.
		 * @return {@link RoleId} that obtains its value using pull-mode.
		 */
		static RoleId pull(VaultToken initialToken) {

			Assert.notNull(initialToken, "Initial token must not be null");

			return new AppRoleTokens.Pull(initialToken);
		}

		/**
		 * Create a {@link RoleId} that encapsulates a static {@code roleId}.
		 *
		 * @param roleId must not be {@literal null} or empty.
		 * @return {@link RoleId} that encapsulates a static {@code roleId}.
		 */
		static RoleId provided(String roleId) {

			Assert.hasText(roleId, "RoleId must not be null or empty");

			return new Provided(roleId);
		}
	}

	/**
	 * SecretId type encapsulating how the secretId is actually obtained. Provides factory
	 * methods to obtain a {@link SecretId} by wrapping, pull-mode or whether to use a
	 * string literal.
	 *
	 * @since 2.0
	 */
	public interface SecretId {

		/**
		 * Create a {@link SecretId} object that obtains its value from unwrapping a
		 * response using the {@link VaultToken initial token} from a Cubbyhole.
		 *
		 * @param initialToken must not be {@literal null}.
		 * @return {@link SecretId} object that obtains its value from unwrapping a
		 * response using the {@link VaultToken initial token}.
		 * @see org.springframework.vault.client.VaultResponses#unwrap(String, Class)
		 */
		static SecretId wrapped(VaultToken initialToken) {

			Assert.notNull(initialToken, "Initial token must not be null");

			return new AppRoleTokens.Wrapped(initialToken);
		}

		/**
		 * Create a {@link SecretId} that obtains its value using pull-mode, specifying a
		 * {@link VaultToken initial token}. The token policy must allow reading the
		 * SecretId from {@code auth/approle/role/(role-name)/secret-id}.
		 *
		 * @param initialToken must not be {@literal null}.
		 * @return {@link SecretId} that obtains its value using pull-mode.
		 */
		static SecretId pull(VaultToken initialToken) {

			Assert.notNull(initialToken, "Initial token must not be null");

			return new AppRoleTokens.Pull(initialToken);
		}

		/**
		 * Create a {@link SecretId} that encapsulates a static {@code secretId}.
		 *
		 * @param secretId must not be {@literal null} or empty.
		 * @return {@link SecretId} that encapsulates a static {@code SecretId}.
		 */
		static SecretId provided(String secretId) {

			Assert.hasText(secretId, "SecretId must not be null or empty");

			return new Provided(secretId);
		}

		/**
		 * Create a {@link SecretId} that represents an absent secretId. Using this object
		 * will not send a secretId during AppRole login.
		 *
		 * @return a {@link SecretId} that represents an absent secretId
		 */
		static SecretId absent() {
			return AbsentSecretId.INSTANCE;
		}
	}
}
