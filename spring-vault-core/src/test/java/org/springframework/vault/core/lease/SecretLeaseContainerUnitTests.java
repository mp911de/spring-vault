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
package org.springframework.vault.core.lease;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.vault.VaultException;
import org.springframework.vault.client.VaultResponses;
import org.springframework.vault.core.RestOperationsCallback;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.lease.domain.Lease;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.AfterSecretLeaseRevocationEvent;
import org.springframework.vault.core.lease.event.BeforeSecretLeaseRevocationEvent;
import org.springframework.vault.core.lease.event.LeaseListenerAdapter;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SecretLeaseContainer}.
 *
 * @author Mark Paluch
 * @author Steven Swor
 */
@RunWith(MockitoJUnitRunner.class)
public class SecretLeaseContainerUnitTests {

	@Mock
	private VaultOperations vaultOperations;

	@Mock
	private TaskScheduler taskScheduler;

	@Mock
	private ScheduledFuture scheduledFuture;

	@Mock
	private LeaseListenerAdapter leaseListenerAdapter;

	@Captor
	private ArgumentCaptor<SecretLeaseEvent> captor;

	private RequestedSecret requestedSecret = RequestedSecret.renewable("my-secret");

	private RequestedSecret rotatingGenericSecret = RequestedSecret
			.rotating("rotating-generic");

	private SecretLeaseContainer secretLeaseContainer;

	@Before
	public void before() throws Exception {

		secretLeaseContainer = new SecretLeaseContainer(vaultOperations, taskScheduler);
		secretLeaseContainer.addLeaseListener(leaseListenerAdapter);
		secretLeaseContainer.addErrorListener(leaseListenerAdapter);
		secretLeaseContainer.afterPropertiesSet();
	}

	@Test
	public void shouldSetProperties() {

		secretLeaseContainer.setMinRenewal(Duration.ofMinutes(2));
		secretLeaseContainer.setExpiryThreshold(Duration.ofMinutes(3));

		assertThat(secretLeaseContainer.getMinRenewal().getSeconds()).isEqualTo(120);
		assertThat(secretLeaseContainer.getMinRenewalSeconds()).isEqualTo(120);
		assertThat(secretLeaseContainer.getExpiryThreshold().getSeconds()).isEqualTo(180);
		assertThat(secretLeaseContainer.getExpiryThresholdSeconds()).isEqualTo(180);
	}

	@Test
	public void shouldWorkIfNoSecretsRequested() {

		secretLeaseContainer.start();

		verifyZeroInteractions(leaseListenerAdapter);
	}

	@Test
	public void shouldWorkIfNoSecretsFound() {

		secretLeaseContainer.start();

		secretLeaseContainer.requestRenewableSecret(requestedSecret.getPath());

		verifyZeroInteractions(leaseListenerAdapter);
	}

	@Test
	public void shouldAcceptSecretsWithoutLease() {

		VaultResponse secrets = new VaultResponse();
		secrets.setData(Collections.singletonMap("key", (Object) "value"));

		when(vaultOperations.read(requestedSecret.getPath())).thenReturn(secrets);

		secretLeaseContainer.addRequestedSecret(requestedSecret);
		secretLeaseContainer.start();

		verifyZeroInteractions(taskScheduler);
		verify(leaseListenerAdapter).onLeaseEvent(captor.capture());

		SecretLeaseCreatedEvent leaseCreatedEvent = (SecretLeaseCreatedEvent) captor
				.getValue();

		assertThat(leaseCreatedEvent.getSource()).isEqualTo(requestedSecret);
		assertThat(leaseCreatedEvent.getLease()).isNotNull();
		assertThat(leaseCreatedEvent.getSecrets()).containsKey("key");
	}

	@Test
	public void shouldAcceptSecretsWithStaticLease() {

		VaultResponse secrets = new VaultResponse();
		secrets.setLeaseId("lease");
		secrets.setRenewable(false);
		secrets.setData(Collections.singletonMap("key", (Object) "value"));

		when(vaultOperations.read(requestedSecret.getPath())).thenReturn(secrets);

		secretLeaseContainer.addRequestedSecret(requestedSecret);
		secretLeaseContainer.start();

		verifyZeroInteractions(taskScheduler);
		verify(leaseListenerAdapter).onLeaseEvent(captor.capture());

		SecretLeaseCreatedEvent leaseCreatedEvent = (SecretLeaseCreatedEvent) captor
				.getValue();

		assertThat(leaseCreatedEvent.getSource()).isEqualTo(requestedSecret);
		assertThat(leaseCreatedEvent.getLease()).isNotNull();
		assertThat(leaseCreatedEvent.getSecrets()).containsKey("key");
	}

	@Test
	public void shouldPropagateErrorsToListenerOnInitialRetrieval() {

		VaultException e = new VaultException("error");
		when(vaultOperations.read(requestedSecret.getPath())).thenThrow(e);

		secretLeaseContainer.addRequestedSecret(requestedSecret);

		secretLeaseContainer.start();

		verify(leaseListenerAdapter).onLeaseError(captor.capture(), eq(e));
		verifyNoMoreInteractions(leaseListenerAdapter);

		SecretLeaseEvent leaseEvent = captor.getValue();

		assertThat(leaseEvent.getSource()).isEqualTo(requestedSecret);
		assertThat(leaseEvent.getLease()).isEqualTo(Lease.none());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldAcceptSecretsWithRenewableLease() {

		when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenReturn(
				scheduledFuture);

		when(vaultOperations.read(requestedSecret.getPath())).thenReturn(createSecrets());

		secretLeaseContainer.addRequestedSecret(requestedSecret);
		secretLeaseContainer.start();

		verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
	}

	@Test
	public void shouldRenewLease() {

		prepareRenewal();
		when(vaultOperations.doWithSession(any(RestOperationsCallback.class)))
				.thenReturn(getResponseEntity("new_lease", true, 70, HttpStatus.OK));

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		captor.getValue().run();
		verifyZeroInteractions(scheduledFuture);
		verify(taskScheduler, times(2)).schedule(captor.capture(), any(Trigger.class));
	}

	@Test
	public void shouldRotateNonRenewableLease() {

		final List<SecretLeaseEvent> events = new ArrayList<SecretLeaseEvent>();
		when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenReturn(
				scheduledFuture);

		when(vaultOperations.read(requestedSecret.getPath())).thenReturn(
				createSecrets("key", "value", false),
				createSecrets("key", "value2", false));

		secretLeaseContainer.addRequestedSecret(RequestedSecret.rotating(requestedSecret
				.getPath()));
		secretLeaseContainer.addLeaseListener(new LeaseListenerAdapter() {
			@Override
			public void onLeaseEvent(SecretLeaseEvent leaseEvent) {
				events.add(leaseEvent);
			}
		});

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		captor.getValue().run();
		verify(taskScheduler, times(2)).schedule(captor.capture(), any(Trigger.class));

		assertThat(events).hasSize(3);
		assertThat(events.get(0)).isInstanceOf(SecretLeaseCreatedEvent.class);
		assertThat(events.get(1)).isInstanceOf(SecretLeaseExpiredEvent.class);
		assertThat(events.get(2)).isInstanceOf(SecretLeaseCreatedEvent.class);

		SecretLeaseCreatedEvent rotated = (SecretLeaseCreatedEvent) events.get(2);
		assertThat(rotated.getSecrets()).containsEntry("key", "value2");
	}

	@Test
	public void shouldRotateGenericSecret() {

		when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenReturn(
				scheduledFuture);

		when(vaultOperations.read(rotatingGenericSecret.getPath())).thenReturn(
				createGenericSecrets(Collections.singletonMap("key", (Object) "value")),
				createGenericSecrets(Collections.singletonMap("foo", (Object) "bar")));

		secretLeaseContainer.addRequestedSecret(rotatingGenericSecret);

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		captor.getValue().run();
		verifyZeroInteractions(scheduledFuture);
		verify(taskScheduler, times(2)).schedule(captor.capture(), any(Trigger.class));

		ArgumentCaptor<SecretLeaseCreatedEvent> createdEvents = ArgumentCaptor
				.forClass(SecretLeaseCreatedEvent.class);
		verify(leaseListenerAdapter, times(3)).onLeaseEvent(createdEvents.capture());

		List<SecretLeaseCreatedEvent> events = createdEvents.getAllValues();

		assertThat(events).hasSize(3);
		assertThat(events.get(0)).isInstanceOf(SecretLeaseCreatedEvent.class);
		assertThat(events.get(0).getSecrets()).containsOnlyKeys("key");

		assertThat(events.get(1)).isInstanceOf(SecretLeaseExpiredEvent.class);

		assertThat(events.get(2)).isInstanceOf(SecretLeaseCreatedEvent.class);
		assertThat(events.get(2).getSecrets()).containsOnlyKeys("foo");
	}

	@Test
	public void shouldNotRenewExpiringLease() {

		prepareRenewal();
		when(vaultOperations.doWithSession(any(RestOperationsCallback.class)))
				.thenReturn(getResponseEntity("new_lease", true, 5, HttpStatus.OK));

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		captor.getValue().run();
		verifyZeroInteractions(scheduledFuture);
		verify(taskScheduler, times(1)).schedule(captor.capture(), any(Trigger.class));
		verify(leaseListenerAdapter).onLeaseEvent(any(SecretLeaseCreatedEvent.class));
		verify(leaseListenerAdapter).onLeaseEvent(any(SecretLeaseExpiredEvent.class));
	}

	@Test
	public void shouldNotRotateExpiringLease() {

		when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenReturn(
				scheduledFuture);

		VaultResponse first = createSecrets();
		VaultResponse second = createSecrets();
		second.setData(Collections.singletonMap("foo", (Object) "bar"));

		when(vaultOperations.read(requestedSecret.getPath())).thenReturn(first, second);
		when(vaultOperations.doWithSession(any(RestOperationsCallback.class)))
				.thenReturn(getResponseEntity("new_lease", true, 5, HttpStatus.OK));

		secretLeaseContainer.requestRotatingSecret("my-secret");

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		captor.getValue().run();
		verify(taskScheduler, times(2)).schedule(captor.capture(), any(Trigger.class));

		ArgumentCaptor<SecretLeaseCreatedEvent> createdEvents = ArgumentCaptor
				.forClass(SecretLeaseCreatedEvent.class);
		verify(leaseListenerAdapter, times(3)).onLeaseEvent(createdEvents.capture());

		List<SecretLeaseCreatedEvent> events = createdEvents.getAllValues();

		assertThat(events).hasSize(3);
		assertThat(events.get(0)).isInstanceOf(SecretLeaseCreatedEvent.class);
		assertThat(events.get(0).getSecrets()).containsOnlyKeys("key");

		assertThat(events.get(1)).isInstanceOf(SecretLeaseExpiredEvent.class);

		assertThat(events.get(2)).isInstanceOf(SecretLeaseCreatedEvent.class);
		assertThat(events.get(2).getSecrets()).containsOnlyKeys("foo");
	}

	@Test
	public void scheduleRenewalShouldApplyExpiryThreshold() {

		prepareRenewal();

		secretLeaseContainer.start();

		ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
		verify(taskScheduler).schedule(any(Runnable.class), captor.capture());

		Date nextExecutionTime = captor.getValue().nextExecutionTime(null);
		assertThat(nextExecutionTime).isBetween(
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(35)),
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(41)));
	}

	@Test
	public void shouldPublishRenewalErrors() {

		prepareRenewal();
		when(vaultOperations.doWithSession(any(RestOperationsCallback.class))).thenThrow(
				new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT));

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(runnableCaptor.capture(), any(Trigger.class));

		runnableCaptor.getValue().run();

		verify(leaseListenerAdapter).onLeaseEvent(any(SecretLeaseCreatedEvent.class));
		verify(leaseListenerAdapter).onLeaseError(captor.capture(),
				any(VaultException.class));
		verifyNoMoreInteractions(leaseListenerAdapter);

		SecretLeaseEvent leaseEvent = captor.getValue();

		assertThat(leaseEvent.getSource()).isEqualTo(requestedSecret);
		assertThat(leaseEvent.getLease()).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subsequentScheduleRenewalShouldApplyExpiryThreshold() {

		prepareRenewal();

		when(vaultOperations.doWithSession(any(RestOperationsCallback.class)))
				.thenReturn(getResponseEntity("new_lease", true, 70, HttpStatus.OK));

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(runnableCaptor.capture(), any(Trigger.class));

		runnableCaptor.getValue().run();

		ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
		verify(taskScheduler, times(2)).schedule(any(Runnable.class), captor.capture());

		assertThat(captor.getAllValues().get(0).nextExecutionTime(null)).isBetween(
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(35)),
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(41)));

		assertThat(captor.getAllValues().get(1).nextExecutionTime(null)).isBetween(
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(9)),
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(11)));
	}

	@Test
	public void scheduleRenewalShouldTriggerOnlyOnce() {

		prepareRenewal();

		secretLeaseContainer.start();

		ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
		verify(taskScheduler).schedule(any(Runnable.class), captor.capture());

		Trigger trigger = captor.getValue();

		assertThat(trigger.nextExecutionTime(null)).isNotNull();
		assertThat(trigger.nextExecutionTime(null)).isNull();
	}

	@Test
	public void subsequentStartShouldNoOp() {

		prepareRenewal();

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		secretLeaseContainer.start();

		verifyNoMoreInteractions(scheduledFuture);
		verifyNoMoreInteractions(taskScheduler);
	}

	@Test
	public void canceledRenewalShouldSkipRenewal() {

		prepareRenewal();

		secretLeaseContainer.start();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));
		verify(vaultOperations).read(anyString());

		secretLeaseContainer.stop();

		verify(scheduledFuture).cancel(false);

		captor.getValue().run();

		verifyNoMoreInteractions(vaultOperations);
	}

	@Test
	public void shouldDisableRenewalOnDisposal() throws Exception {

		prepareRenewal();

		secretLeaseContainer.start();
		secretLeaseContainer.destroy();

		verify(vaultOperations).doWithSession(any(RestOperationsCallback.class));
		verify(scheduledFuture).cancel(false);
		verify(leaseListenerAdapter).onLeaseEvent(any(SecretLeaseCreatedEvent.class));
		verify(leaseListenerAdapter).onLeaseEvent(
				any(BeforeSecretLeaseRevocationEvent.class));
		verify(leaseListenerAdapter).onLeaseEvent(
				any(AfterSecretLeaseRevocationEvent.class));
	}

	@Test
	public void expiredLeaseShouldRenew() {

		prepareRenewal();

		VaultException exception = VaultResponses
				.buildException(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

		when(vaultOperations.doWithSession(any(RestOperationsCallback.class))).thenThrow(
				exception);

		secretLeaseContainer.doRenewLease(requestedSecret, Lease.of("foo", 1, true));

		verify(leaseListenerAdapter).onLeaseEvent(any(SecretLeaseExpiredEvent.class));
		verify(vaultOperations).doWithSession(any(RestOperationsCallback.class));
	}

	@Test
	public void shouldNotRevokeSecretsWithoutLease() throws Exception {

		VaultResponse secrets = new VaultResponse();
		secrets.setData(Collections.singletonMap("key", (Object) "value"));

		when(vaultOperations.read(requestedSecret.getPath())).thenReturn(secrets);

		secretLeaseContainer.addRequestedSecret(requestedSecret);
		secretLeaseContainer.start();

		secretLeaseContainer.destroy();

		verifyZeroInteractions(taskScheduler);

		verify(leaseListenerAdapter, never()).onLeaseEvent(
				any(BeforeSecretLeaseRevocationEvent.class));
		verify(leaseListenerAdapter, never()).onLeaseEvent(
				any(AfterSecretLeaseRevocationEvent.class));
	}

	@Test
	public void shouldRequestRotatingGenericSecrets() {

		when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenReturn(
				scheduledFuture);

		when(vaultOperations.read(rotatingGenericSecret.getPath())).thenReturn(
				createGenericSecrets());

		secretLeaseContainer.addRequestedSecret(rotatingGenericSecret);
		secretLeaseContainer.start();

		verify(leaseListenerAdapter).onLeaseEvent(captor.capture());

		SecretLeaseCreatedEvent leaseCreatedEvent = (SecretLeaseCreatedEvent) captor
				.getValue();

		assertThat(leaseCreatedEvent.getSource()).isEqualTo(rotatingGenericSecret);
		assertThat(leaseCreatedEvent.getLease()).isNotNull();
		assertThat(leaseCreatedEvent.getSecrets()).containsKey("key");
	}

	@SuppressWarnings("unchecked")
	private void prepareRenewal() {

		when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenReturn(
				scheduledFuture);

		when(vaultOperations.read(requestedSecret.getPath())).thenReturn(createSecrets());

		secretLeaseContainer.addRequestedSecret(requestedSecret);
	}

	private ResponseEntity<Map<String, Object>> getResponseEntity(String leaseId,
			Boolean renewable, Integer leaseDuration, HttpStatus httpStatus) {

		Map<String, Object> body = new HashMap<String, Object>();
		body.put("lease_id", leaseId);
		body.put("renewable", renewable);
		body.put("lease_duration", leaseDuration);

		return getEntity(body, httpStatus);
	}

	private ResponseEntity<Map<String, Object>> getEntity(Map<String, Object> body,
			HttpStatus status) {

		return new ResponseEntity<Map<String, Object>>(body, status);
	}

	private VaultResponse createSecrets() {
		return createSecrets("key", "value", true);
	}

	private VaultResponse createSecrets(String key, String value, boolean renewable) {

		VaultResponse secrets = new VaultResponse();

		secrets.setLeaseId("lease");
		secrets.setRenewable(renewable);
		secrets.setLeaseDuration(100);
		secrets.setData(Collections.singletonMap(key, (Object) value));

		return secrets;
	}

	private VaultResponse createGenericSecrets() {
		return createGenericSecrets(Collections.singletonMap("key", (Object) "value"));
	}

	private VaultResponse createGenericSecrets(Map<String, Object> data) {

		VaultResponse secrets = new VaultResponse();

		secrets.setRenewable(false);
		secrets.setLeaseDuration(100);
		secrets.setData(data);

		return secrets;
	}
}
