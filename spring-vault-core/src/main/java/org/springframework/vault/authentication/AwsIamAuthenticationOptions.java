/*
 * Copyright 2017-2020 the original author or authors.
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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Authentication options for {@link AwsIamAuthentication}.
 * <p>
 * Authentication options provide the path, a {@link AWSCredentialsProvider} optional role
 * and server name ({@literal Vault-AWS-IAM-Server-ID} header).
 * {@link AwsIamAuthenticationOptions} can be constructed using {@link #builder()}.
 * Instances of this class are immutable once constructed.
 *
 * @author Mark Paluch
 * @since 1.1
 * @see AwsIamAuthentication
 * @see #builder()
 */
public class AwsIamAuthenticationOptions {

	public static final URI DEFAULT_STS_ENDPOINT = URI.create("https://sts.amazonaws.com/");

	public static final String DEFAULT_AWS_AUTHENTICATION_PATH = "aws";

	/**
	 * Path of the aws authentication backend mount.
	 */
	private final String path;

	/**
	 * Credential provider.
	 */
	private final AWSCredentialsProvider credentialsProvider;

	/**
	 * Name of the role against which the login is being attempted. If role is not
	 * specified, the friendly name (i.e., role name or username) of the IAM principal
	 * authenticated. If a matching role is not found, login fails.
	 */
	@Nullable
	private final String role;

	/**
	 * Server name to mitigate risk of replay attacks, preferably set to Vault server's
	 * DNS name. Used for {@literal Vault-AWS-IAM-Server-ID} header.
	 */
	@Nullable
	private final String serverId;

	/**
	 * STS server URI.
	 */
	private final URI endpointUri;

	private AwsIamAuthenticationOptions(String path,
			AWSCredentialsProvider credentialsProvider, @Nullable String role,
			@Nullable String serverId, URI endpointUri) {

		this.path = path;
		this.credentialsProvider = credentialsProvider;
		this.role = role;
		this.serverId = serverId;
		this.endpointUri = endpointUri;
	}

	/**
	 * @return a new {@link AwsIamAuthenticationOptionsBuilder}.
	 */
	public static AwsIamAuthenticationOptionsBuilder builder() {
		return new AwsIamAuthenticationOptionsBuilder();
	}

	/**
	 * @return the path of the aws authentication backend mount.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the credentials provider to obtain AWS credentials.
	 */
	public AWSCredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	/**
	 * @return the role, may be {@literal null} if none.
	 */
	@Nullable
	public String getRole() {
		return role;
	}

	/**
	 * @return Server name to mitigate risk of replay attacks, preferably set to Vault
	 * server's DNS name, may be {@literal null}. Used for
	 * {@literal Vault-AWS-IAM-Server-ID} header.
	 * @since 2.0
	 */
	@Nullable
	public String getServerId() {
		return serverId;
	}

	/**
	 * @return Server name to mitigate risk of replay attacks, preferably set to Vault
	 * server's DNS name, may be {@literal null}.
	 * @deprecated since 2.0, renamed to {@link #getServerId()}.
	 */
	@Nullable
	@Deprecated
	public String getServerName() {
		return serverId;
	}

	/**
	 * @return STS server URI.
	 */
	public URI getEndpointUri() {
		return endpointUri;
	}

	/**
	 * @return Whether or not these options are configured against the default
	 * global STS endpoint
	 *
	 * @return a boolean value
	 */
	public boolean hasDefaultEndpoint() {
		return DEFAULT_STS_ENDPOINT.getHost().equals(this.endpointUri.getHost());
	}

	/**
	 * Builder for {@link AwsIamAuthenticationOptions}.
	 */
	public static class AwsIamAuthenticationOptionsBuilder {

		private String path = DEFAULT_AWS_AUTHENTICATION_PATH;

		@Nullable
		private AWSCredentialsProvider credentialsProvider;

		@Nullable
		private String role;

		@Nullable
		private String serverId;

		private URI endpointUri = DEFAULT_STS_ENDPOINT;

		AwsIamAuthenticationOptionsBuilder() {
		}

		/**
		 * Configure the mount path, defaults to {@literal aws}.
		 *
		 * @param path must not be empty or {@literal null}.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 */
		public AwsIamAuthenticationOptionsBuilder path(String path) {

			Assert.hasText(path, "Path must not be empty");

			this.path = path;
			return this;
		}

		/**
		 * Configure static AWS credentials, required to calculate the signature. Either
		 * use static credentials or provide a
		 * {@link #credentialsProvider(AWSCredentialsProvider) credentials provider}.
		 *
		 * @param credentials must not be {@literal null}.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 * @see #credentialsProvider(AWSCredentialsProvider)
		 */
		public AwsIamAuthenticationOptionsBuilder credentials(
				AWSCredentials credentials) {

			Assert.notNull(credentials, "Credentials must not be null");

			return credentialsProvider(new AWSStaticCredentialsProvider(credentials));
		}

		/**
		 * Configure an {@link AWSCredentialsProvider}, required to calculate the
		 * signature. Alternatively, configure static {@link #credentials(AWSCredentials)
		 * credentials}.
		 *
		 * @param credentialsProvider must not be {@literal null}.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 * @see #credentials(AWSCredentials)
		 */
		public AwsIamAuthenticationOptionsBuilder credentialsProvider(
				AWSCredentialsProvider credentialsProvider) {

			Assert.notNull(credentialsProvider,
					"AWSCredentialsProvider must not be null");

			this.credentialsProvider = credentialsProvider;
			return this;
		}

		/**
		 * Configure the name of the role against which the login is being attempted. If
		 * role is not specified, the friendly name (i.e., role name or username) of the
		 * IAM principal authenticated. If a matching role is not found, login fails.
		 *
		 * @param role must not be empty or {@literal null}.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 */
		public AwsIamAuthenticationOptionsBuilder role(String role) {

			Assert.hasText(role, "Role must not be null or empty");

			this.role = role;
			return this;
		}

		/**
		 * Configure a server name (used for {@literal Vault-AWS-IAM-Server-ID}) that is
		 * included in the signature to mitigate the risk of replay attacks. Preferably
		 * use the Vault server DNS name.
		 *
		 * @param serverId must not be {@literal null} or empty.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 * @since 2.1
		 */
		public AwsIamAuthenticationOptionsBuilder serverId(String serverId) {

			Assert.hasText(serverId, "Server name must not be null or empty");

			this.serverId = serverId;
			return this;
		}

		/**
		 * Configure a server name that is included in the signature to mitigate the risk
		 * of replay attacks. Preferably use the Vault server DNS name.
		 *
		 * @param serverName must not be {@literal null} or empty.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 */
		public AwsIamAuthenticationOptionsBuilder serverName(String serverName) {
			return serverId(serverName);
		}

		/**
		 * Configure an endpoint URI of the STS API, defaults to
		 * {@literal https://sts.amazonaws.com/}.
		 *
		 * @param endpointUri must not be {@literal null}.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 */
		public AwsIamAuthenticationOptionsBuilder endpointUri(URI endpointUri) {

			Assert.notNull(endpointUri, "Endpoint URI must not be null");

			this.endpointUri = endpointUri;
			return this;
		}

		/**
		 * Copy all the settings from the given original options.
		 * {@literal https://sts.amazonaws.com/}.
		 *
		 * @param other must not be {@literal null}.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 */
		public AwsIamAuthenticationOptionsBuilder options(AwsIamAuthenticationOptions other) {

			Assert.notNull(other, "Options must not be null");

			this.endpointUri = other.endpointUri;
			this.credentialsProvider = other.credentialsProvider;
			this.path  = other.path;
			this.role = other.role;
			this.serverId = other.serverId;
			return this;
		}

		/**
		 * Build a new {@link AwsIamAuthenticationOptions} instance.
		 *
		 * @return a new {@link AwsIamAuthenticationOptions}.
		 */
		public AwsIamAuthenticationOptions build() {

			Assert.state(credentialsProvider != null,
					"Credentials or CredentialProvider must not be null");

			return new AwsIamAuthenticationOptions(path, credentialsProvider, role,
					serverId, endpointUri);
		}
	}
}
