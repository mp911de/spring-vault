/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.vault.core;

import org.springframework.vault.support.*;

import java.util.List;

/**
 * Interface that specifies operations using the {@code transform} backend.
 *
 * @author Lauren Voswinkel
 * @see <a href="https://www.vaultproject.io/docs/secrets/transform/index.html">Transform
 * Secrets Engine</a>
 * @since 2.3
 */
public interface VaultTransformOperations {
	/**
	 * Encodes the provided plaintext using the named role.
	 * @param roleName must not be empty or {@literal null}.
	 * @param plaintext must not be empty or {@literal null}.
	 * @return cipher text.
	 */
	String encode(String roleName, String plaintext);

	/**
	 * Encodes the provided plaintext using the named role.
	 * @param roleName must not be empty or {@literal null}.
	 * @param plaintext must not be {@literal null}.
	 * @return cipher text.
	 */
	TransformCiphertext encode(String roleName, TransformPlaintext plaintext);

	/**
	 * Encodes the provided plaintext using the named role.
	 * @param roleName must not be empty or {@literal null}.
	 * @param plaintext must not be empty or {@literal null}.
	 * @param transformRequest must not be {@literal null}. Use
	 * {@link VaultTransformContext#empty()} if no request options provided.
	 * @return cipher text.
	 */
	TransformCiphertext encode(String roleName, byte[] plaintext, VaultTransformContext transformRequest);

	/**
	 * Encode the provided batch of plaintext using the role given and transformation in
	 * each list item. The encryption is done using transformation secret backend's batch
	 * operation.
	 * @param roleName must not be empty or {@literal null}.
	 * @param batchRequest a list of {@link Plaintext} which includes plaintext and an
	 * optional context.
	 * @return the encrypted result in the order of {@code batchRequest} plaintexts.
	 */
	List<VaultTransformEncodeResult> encode(String roleName, List<TransformPlaintext> batchRequest);

	/**
	 * Decode the provided ciphertext using the named role.
	 * @param roleName must not be empty or {@literal null}.
	 * @param ciphertext must not be empty or {@literal null}.
	 * @return plain text.
	 */
	String decode(String roleName, String ciphertext);

	/**
	 * Decode the provided ciphertext using the named role.
	 * @param roleName must not be empty or {@literal null}.
	 * @param ciphertext must not be {@literal null}.
	 * @return plain text.
	 */
	TransformPlaintext decode(String roleName, TransformCiphertext ciphertext);

	/**
	 * Decode the provided ciphertext using the named role.
	 * @param roleName must not be empty or {@literal null}.
	 * @param ciphertext must not be empty or {@literal null}.
	 * @param transformContext must not be {@literal null}. Use
	 * {@link VaultTransformContext#empty()} if no request options provided.
	 * @return plain text.
	 */
	String decode(String roleName, String ciphertext, VaultTransformContext transformContext);

	/**
	 * Decode the provided batch of ciphertext using the role given and transformation in
	 * each list item. The decryption is done using transformation secret backend's batch
	 * operation.
	 * @param roleName must not be empty or {@literal null}.
	 * @param batchRequest a list of {@link Ciphertext} which includes plaintext and an
	 * optional context.
	 * @return the decrypted result in the order of {@code batchRequest} ciphertexts.
	 */
	List<VaultTransformDecodeResult> decode(String roleName, List<TransformCiphertext> batchRequest);
}
