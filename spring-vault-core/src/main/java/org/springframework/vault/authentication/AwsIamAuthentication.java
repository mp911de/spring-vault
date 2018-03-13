/*
 * Copyright 2017-2018 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.http.HttpMethodName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.vault.VaultException;
import org.springframework.vault.client.VaultResponses;
import org.springframework.vault.exceptions.VaultClientException;
import org.springframework.vault.exceptions.VaultHttpException;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;

/**
 * AWS IAM authentication using signed HTTP requests to query the current identity.
 * <p>
 * AWS IAM authentication creates a {@link AWS4Signer signed} HTTP request that is
 * executed by Vault to get the identity of the signer using AWS STS
 * {@literal GetCallerIdentity}. A signature requires
 * {@link com.amazonaws.auth.AWSCredentials} to calculate the signature.
 * <p>
 * This authentication requires AWS' Java SDK to sign request parameters and calculate the
 * signature key. Using an appropriate {@link com.amazonaws.auth.AWSCredentialsProvider}
 * allows authentication within AWS-EC2 instances with an assigned profile, within ECS and
 * Lambda instances.
 *
 * @author Mark Paluch
 * @since 1.1
 * @see AwsIamAuthenticationOptions
 * @see com.amazonaws.auth.AWSCredentialsProvider
 * @see RestOperations
 * @see <a href="https://www.vaultproject.io/docs/auth/aws.html">Auth Backend: aws
 * (IAM)</a>
 * @see <a
 * href="http://docs.aws.amazon.com/STS/latest/APIReference/API_GetCallerIdentity.html">AWS:
 * GetCallerIdentity</a>
 */
public class AwsIamAuthentication implements ClientAuthentication {

	private static final Log logger = LogFactory.getLog(AwsIamAuthentication.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final String REQUEST_BODY = "Action=GetCallerIdentity&Version=2011-06-15";

	private static final String REQUEST_BODY_BASE64_ENCODED = Base64Utils
			.encodeToString(REQUEST_BODY.getBytes());

	private final AwsIamAuthenticationOptions options;

	private final RestOperations vaultRestOperations;

	/**
	 * Create a new {@link AwsIamAuthentication} specifying
	 * {@link AwsIamAuthenticationOptions}, a Vault and an AWS-Metadata-specific
	 * {@link RestOperations} .
	 *
	 * @param options must not be {@literal null}.
	 * @param vaultRestOperations must not be {@literal null}.
	 */
	public AwsIamAuthentication(AwsIamAuthenticationOptions options,
			RestOperations vaultRestOperations) {

		Assert.notNull(options, "AwsIamAuthenticationOptions must not be null");
		Assert.notNull(vaultRestOperations, "Vault RestOperations must not be null");

		this.options = options;
		this.vaultRestOperations = vaultRestOperations;
	}

	@Override
	public VaultToken login() throws VaultException {
		return createTokenUsingAwsIam();
	}

	@SuppressWarnings("unchecked")
	private VaultToken createTokenUsingAwsIam() {

		Map<String, String> login = createRequestBody(this.options);

		try {

			VaultResponse response = this.vaultRestOperations.postForObject(
					"auth/{mount}/login", login, VaultResponse.class, options.getPath());

			Assert.state(response != null && response.getAuth() != null,
					"Auth field must not be null");

			if (logger.isDebugEnabled()) {

				if (response.getAuth().get("metadata") instanceof Map) {
					Map<Object, Object> metadata = (Map<Object, Object>) response
							.getAuth().get("metadata");
					logger.debug(String
							.format("Login successful using AWS-IAM authentication for user id %s, ARN %s",
									metadata.get("client_user_id"),
									metadata.get("canonical_arn")));
				}
				else {
					logger.debug("Login successful using AWS-IAM authentication");
				}
			}

			return LoginTokenUtil.from(response.getAuth());
		}
		catch (HttpStatusCodeException e) {
			throw new VaultHttpException(String.format("Cannot login using AWS-IAM: %s",
					VaultResponses.getError(e.getResponseBodyAsString())), e);
		}
	}

	/**
	 * Create the request body to perform a Vault login using the AWS-IAM authentication
	 * method.
	 * 
	 * @param options must not be {@literal null}.
	 * @return the map containing body key-value pairs.
	 */
	protected static Map<String, String> createRequestBody(
			AwsIamAuthenticationOptions options) {

		Map<String, String> login = new HashMap<>();

		login.put("iam_http_request_method", "POST");
		login.put("iam_request_url", Base64Utils.encodeToString(options.getEndpointUri()
				.toString().getBytes()));
		login.put("iam_request_body", REQUEST_BODY_BASE64_ENCODED);

		String headerJson = getSignedHeaders(options);

		login.put("iam_request_headers",
				Base64Utils.encodeToString(headerJson.getBytes()));

		if (!StringUtils.isEmpty(options.getRole())) {
			login.put("role", options.getRole());
		}
		return login;
	}

	private static String getSignedHeaders(AwsIamAuthenticationOptions options) {

		Map<String, String> headers = createIamRequestHeaders(options);

		AWS4Signer signer = new AWS4Signer();

		DefaultRequest<String> request = new DefaultRequest<>("sts");

		request.setContent(new ByteArrayInputStream(REQUEST_BODY.getBytes()));
		request.setHeaders(headers);
		request.setHttpMethod(HttpMethodName.POST);
		request.setEndpoint(options.getEndpointUri());

		signer.setServiceName(request.getServiceName());
		signer.sign(request, options.getCredentialsProvider().getCredentials());

		Map<String, Object> map = new LinkedHashMap<>();

		for (Entry<String, String> entry : request.getHeaders().entrySet()) {
			map.put(entry.getKey(), Collections.singletonList(entry.getValue()));
		}

		try {
			return OBJECT_MAPPER.writeValueAsString(map);
		}
		catch (JsonProcessingException e) {
			throw new VaultClientException("Cannot serialize headers to JSON", e);
		}
	}

	private static Map<String, String> createIamRequestHeaders(
			AwsIamAuthenticationOptions options) {

		Map<String, String> headers = new LinkedHashMap<>();

		headers.put(HttpHeaders.CONTENT_LENGTH, "" + REQUEST_BODY.length());
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

		if (StringUtils.hasText(options.getServerId())) {
			headers.put("X-Vault-AWS-IAM-Server-ID", options.getServerId());
		}

		return headers;
	}
}
