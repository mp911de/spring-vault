/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.vault.core.lease;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.vault.core.VaultOperations;

/**
 * An {@link AbstractTestExecutionListener} which will delete the sample
 * rotating secret from Vault after the test finishes.
 *
 * @author Steven Swor
 */
public class DeleteSecretsAfterTestFinishesListener
		extends AbstractTestExecutionListener {

	/**
	 * Deletes the sample rotating secret from Vault after the test finishes.
	 *
	 * @param testContext the test context. May not be {@literal null}.
	 * @throws Exception if something bad happens
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		VaultOperations vaultOperations = testContext.getApplicationContext()
				.getBean(VaultOperations.class);
		vaultOperations.delete("secret/rotating");

		assertThat(vaultOperations.read("secret/rotating")).isNull();
	}

}
