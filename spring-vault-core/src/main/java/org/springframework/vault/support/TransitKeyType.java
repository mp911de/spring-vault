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
package org.springframework.vault.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration to specify the type of the transit key. Intended for use with
 * {@link org.springframework.vault.core.VaultTransitOperations}
 *
 * @author Sven Schürmann
 * @author Mark Paluch
 */
@Getter
@RequiredArgsConstructor
public enum TransitKeyType {

	ENCRYPTION_KEY("encryption-key"), SIGNING_KEY("signing-key"), HMAC_KEY("hmac-key");

	final String value;
}
