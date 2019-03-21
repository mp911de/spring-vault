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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;

import org.springframework.util.Assert;

/**
 * Authentication options for {@link AwsIamAuthentication}.
 * <p>
 * Authentication options provide the path, a {@link AWSCredentialsProvider} optional role
 * and server name. {@link AwsIamAuthenticationOptions} can be constructed using
 * {@link #builder()}. Instances of this class are immutable once constructed.
 *
 * @author Mark Paluch
 * @since 1.1
 * @see AwsIamAuthentication
 * @see #builder()
 */
public class AwsIamAuthenticationOptions {

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
	 * EC2 instance role name. May be {@literal null} if none.
	 */
	private final String role;

	/**
	 * Server name to mitigate risk of replay attacks, preferably set to Vault server's
	 * DNS name.
	 */
	private final String serverName;

	/**
	 * STS server URI.
	 */
	private final URI endpointUri;

	private AwsIamAuthenticationOptions(String path,
			AWSCredentialsProvider credentialsProvider, String role, String serverName,
			URI endpointUri) {

		this.path = path;
		this.credentialsProvider = credentialsProvider;
		this.role = role;
		this.serverName = serverName;
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
	public String getRole() {
		return role;
	}

	/**
	 * @return Server name to mitigate risk of replay attacks, preferably set to Vault
	 * server's DNS name, may be {@literal null}.
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * @return STS server URI.
	 */
	public URI getEndpointUri() {
		return endpointUri;
	}

	/**
	 * Builder for {@link AwsIamAuthenticationOptions}.
	 */
	public static class AwsIamAuthenticationOptionsBuilder {

		private String path = DEFAULT_AWS_AUTHENTICATION_PATH;
		private AWSCredentialsProvider credentialsProvider;
		private String role;
		private String serverName;
		private URI endpointUri = URI.create("https://sts.amazonaws.com/");

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
		public AwsIamAuthenticationOptionsBuilder credentials(AWSCredentials credentials) {

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

			Assert.notNull(credentialsProvider, "AWSCredentialsProvider must not be null");

			this.credentialsProvider = credentialsProvider;
			return this;
		}

		/**
		 * Configure the name of the role against which the login is being attempted.If
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
		 * Configure a server name that is included in the signature to mitigate the risk
		 * of replay attacks. Preferably use the Vault server DNS name.
		 *
		 * @param serverName must not be {@literal null} or empty.
		 * @return {@code this} {@link AwsIamAuthenticationOptionsBuilder}.
		 */
		public AwsIamAuthenticationOptionsBuilder serverName(String serverName) {

			Assert.hasText(serverName, "Server name must not be null or empty");

			this.serverName = serverName;
			return this;
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
		 * Build a new {@link AwsIamAuthenticationOptions} instance.
		 *
		 * @return a new {@link AwsIamAuthenticationOptions}.
		 */
		public AwsIamAuthenticationOptions build() {

			Assert.state(credentialsProvider != null,
					"Credentials or CredentialProvider must not be null");

			return new AwsIamAuthenticationOptions(path, credentialsProvider, role,
					serverName, endpointUri);
		}
	}
}
