/*
 * Copyright 2018-2020 the original author or authors.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.support.VaultResponse;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link VaultKeyValue2Template} using the versioned Key/Value (k/v
 * version 2) backend.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = VaultIntegrationTestConfiguration.class)
class VaultKeyValueTemplateVersionedIntegrationTests extends AbstractVaultKeyValueTemplateIntegrationTests {

	VaultKeyValueTemplateVersionedIntegrationTests() {
		super("versioned", KeyValueBackend.versioned());
	}

	@Test
	void shouldPatchSecret() {
		final String oldKey = "key";
		final String newKey = "newKey";
		Map<String, String> secret = Collections.singletonMap(oldKey, "value");

		String key = UUID.randomUUID().toString();

		this.kvOperations.put(key, secret);

		Map<String, String> newSecret = Collections.singletonMap(newKey, "newValue");

		assertTrue(this.kvOperations.patch(key, newSecret));

		assertThat(this.kvOperations.list("/")).contains(key);
		VaultResponse vaultResponse = this.kvOperations.get(key);
		assertNotNull(vaultResponse);
		Map<String, Object> data = vaultResponse.getData();
		assertNotNull(data);
		assertThat(data).containsKey(oldKey);
		assertThat(data).containsKey(newKey);
	}

}
