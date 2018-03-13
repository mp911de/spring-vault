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

import org.springframework.http.HttpStatus;
import org.springframework.vault.exceptions.VaultHttpException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Exception thrown if a token self-lookup fails via {@code auth/token/lookup-self}.
 *
 * @author Mark Paluch
 * @since 2.0
 */
public class VaultTokenLookupException extends VaultHttpException {

	/**
	 * Create a {@code VaultException} with the specified detail message.
	 *
	 * @param msg the detail message.
	 */
	public VaultTokenLookupException(String msg, HttpStatusCodeException httpStatus) {
		super(msg, httpStatus);
	}

	public VaultTokenLookupException(final String msg, final WebClientResponseException e) {
		super(msg, e);
	}
}
