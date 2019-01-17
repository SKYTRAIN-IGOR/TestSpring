/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.session.hazelcast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryEventType;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.query.impl.predicates.EqualPredicate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.hazelcast.HazelcastSessionRepository.HazelcastSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link HazelcastSessionRepository}.
 *
 * @author Vedran Pavic
 * @author Aleksandar Stojsavljevic
 */
@RunWith(MockitoJUnitRunner.class)
public class HazelcastSessionRepositoryTests {

	private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private IMap<String, MapSession> sessions;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	private HazelcastSessionRepository repository;

	@Before
	public void setUp() {
		this.repository = new HazelcastSessionRepository(this.sessions);
	}

	@Test
	public void constructorNullHazelcastInstance() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Sessions IMap must not be null");

		new HazelcastSessionRepository(null);
	}

	@Test
	public void createSessionDefaultMaxInactiveInterval() throws Exception {
		HazelcastSession session = this.repository.createSession();

		assertThat(session.getMaxInactiveIntervalInSeconds())
				.isEqualTo(new MapSession().getMaxInactiveIntervalInSeconds());
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void createSessionCustomMaxInactiveInterval() throws Exception {
		int interval = 1;
		this.repository.setDefaultMaxInactiveInterval(interval);

		HazelcastSession session = this.repository.createSession();

		assertThat(session.getMaxInactiveIntervalInSeconds()).isEqualTo(interval);
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void saveNewFlushModeOnSave() {
		HazelcastSession session = this.repository.createSession();
		verifyZeroInteractions(this.sessions);

		this.repository.save(session);
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
	}

	@Test
	public void saveNewFlushModeImmediate() {
		this.repository.setHazelcastFlushMode(HazelcastFlushMode.IMMEDIATE);

		HazelcastSession session = this.repository.createSession();
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
	}

	@Test
	public void saveUpdatedAttributeFlushModeOnSave() {
		HazelcastSession session = this.repository.createSession();
		session.setAttribute("testName", "testValue");
		verifyZeroInteractions(this.sessions);

		this.repository.save(session);
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
	}

	@Test
	public void saveUpdatedAttributeFlushModeImmediate() {
		this.repository.setHazelcastFlushMode(HazelcastFlushMode.IMMEDIATE);

		HazelcastSession session = this.repository.createSession();
		session.setAttribute("testName", "testValue");
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
		verify(this.sessions, times(1)).executeOnKey(eq(session.getId()),
				any(EntryProcessor.class));

		this.repository.save(session);
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void removeAttributeFlushModeOnSave() {
		HazelcastSession session = this.repository.createSession();
		session.removeAttribute("testName");
		verifyZeroInteractions(this.sessions);

		this.repository.save(session);
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
	}

	@Test
	public void removeAttributeFlushModeImmediate() {
		this.repository.setHazelcastFlushMode(HazelcastFlushMode.IMMEDIATE);

		HazelcastSession session = this.repository.createSession();
		session.removeAttribute("testName");
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
		verify(this.sessions, times(1)).executeOnKey(eq(session.getId()),
				any(EntryProcessor.class));

		this.repository.save(session);
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void saveUpdatedLastAccessedTimeFlushModeOnSave() {
		HazelcastSession session = this.repository.createSession();
		session.setLastAccessedTime(System.currentTimeMillis());
		verifyZeroInteractions(this.sessions);

		this.repository.save(session);
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
	}

	@Test
	public void saveUpdatedLastAccessedTimeFlushModeImmediate() {
		this.repository.setHazelcastFlushMode(HazelcastFlushMode.IMMEDIATE);

		HazelcastSession session = this.repository.createSession();
		session.setLastAccessedTime(System.currentTimeMillis());
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
		verify(this.sessions, times(1)).executeOnKey(eq(session.getId()),
				any(EntryProcessor.class));

		this.repository.save(session);
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void saveUpdatedMaxInactiveIntervalInSecondsFlushModeOnSave() {
		HazelcastSession session = this.repository.createSession();
		session.setMaxInactiveIntervalInSeconds(1);
		verifyZeroInteractions(this.sessions);

		this.repository.save(session);
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
	}

	@Test
	public void saveUpdatedMaxInactiveIntervalInSecondsFlushModeImmediate() {
		this.repository.setHazelcastFlushMode(HazelcastFlushMode.IMMEDIATE);

		HazelcastSession session = this.repository.createSession();
		session.setMaxInactiveIntervalInSeconds(1);
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));
		verify(this.sessions, times(1)).executeOnKey(eq(session.getId()),
				any(EntryProcessor.class));

		this.repository.save(session);
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void saveUnchangedFlushModeOnSave() {
		HazelcastSession session = this.repository.createSession();
		this.repository.save(session);
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));

		this.repository.save(session);
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void saveUnchangedFlushModeImmediate() {
		this.repository.setHazelcastFlushMode(HazelcastFlushMode.IMMEDIATE);

		HazelcastSession session = this.repository.createSession();
		verify(this.sessions, times(1)).set(eq(session.getId()),
				eq(session.getDelegate()), isA(Long.class), eq(TimeUnit.SECONDS));

		this.repository.save(session);
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void getSessionNotFound() {
		String sessionId = "testSessionId";

		HazelcastSession session = this.repository.getSession(sessionId);

		assertThat(session).isNull();
		verify(this.sessions, times(1)).get(eq(sessionId));
	}

	@Test
	public void getSessionExpired() {
		MapSession expired = new MapSession();
		expired.setLastAccessedTime(System.currentTimeMillis()
				- (MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS * 1000 + 1000));
		given(this.sessions.get(eq(expired.getId()))).willReturn(expired);

		HazelcastSession session = this.repository.getSession(expired.getId());

		assertThat(session).isNull();
		verify(this.sessions, times(1)).get(eq(expired.getId()));
		verify(this.sessions, times(1)).remove(eq(expired.getId()));
	}

	@Test
	public void getSessionFound() {
		MapSession saved = new MapSession();
		saved.setAttribute("savedName", "savedValue");
		given(this.sessions.get(eq(saved.getId()))).willReturn(saved);

		HazelcastSession session = this.repository.getSession(saved.getId());

		assertThat(session.getId()).isEqualTo(saved.getId());
		assertThat(session.getAttribute("savedName")).isEqualTo("savedValue");
		verify(this.sessions, times(1)).get(eq(saved.getId()));
	}

	@Test
	public void delete() {
		String sessionId = "testSessionId";

		this.repository.delete(sessionId);

		verify(this.sessions, times(1)).remove(eq(sessionId));
	}

	@Test
	public void findByIndexNameAndIndexValueUnknownIndexName() {
		String indexValue = "testIndexValue";

		Map<String, HazelcastSession> sessions = this.repository
				.findByIndexNameAndIndexValue("testIndexName", indexValue);

		assertThat(sessions).isEmpty();
		verifyZeroInteractions(this.sessions);
	}

	@Test
	public void findByIndexNameAndIndexValuePrincipalIndexNameNotFound() {
		String principal = "username";

		Map<String, HazelcastSession> sessions = this.repository
				.findByIndexNameAndIndexValue(
						FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
						principal);

		assertThat(sessions).isEmpty();
		verify(this.sessions, times(1)).values(isA(EqualPredicate.class));
	}

	@Test
	public void findByIndexNameAndIndexValuePrincipalIndexNameFound() {
		String principal = "username";
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal,
				"notused", AuthorityUtils.createAuthorityList("ROLE_USER"));
		List<MapSession> saved = new ArrayList<MapSession>(2);
		MapSession saved1 = new MapSession();
		saved1.setAttribute(SPRING_SECURITY_CONTEXT, authentication);
		saved.add(saved1);
		MapSession saved2 = new MapSession();
		saved2.setAttribute(SPRING_SECURITY_CONTEXT, authentication);
		saved.add(saved2);
		given(this.sessions.values(isA(EqualPredicate.class))).willReturn(saved);

		Map<String, HazelcastSession> sessions = this.repository
				.findByIndexNameAndIndexValue(
						FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
						principal);

		assertThat(sessions).hasSize(2);
		verify(this.sessions, times(1)).values(isA(EqualPredicate.class));
	}

	@Test // gh-1120
	public void getAttributeNamesAndRemove() {
		HazelcastSession session = this.repository.createSession();
		session.setAttribute("attribute1", "value1");
		session.setAttribute("attribute2", "value2");

		for (String attributeName : session.getAttributeNames()) {
			session.removeAttribute(attributeName);
		}

		assertThat(session.getAttributeNames()).isEmpty();
	}

	@Test // gh-1077
	public void entryRemovedPublishEvent() {
		this.repository.setApplicationEventPublisher(this.applicationEventPublisher);
		MapSession oldValue = new MapSession();
		EntryEvent<String, MapSession> entryEvent =
				new EntryEvent<String, MapSession>("Test", null, EntryEventType.REMOVED.getType(), null, oldValue, null);
		this.repository.entryRemoved(entryEvent);
		verify(this.applicationEventPublisher, times(1)).publishEvent(Mockito.any(SessionDeletedEvent.class));
	}


	@Test // gh-1077
	public void entryRemovedWithNullOldValue() {
		MapSession oldValue = null;
		EntryEvent<String, MapSession> entryEvent =
				new EntryEvent<String, MapSession>("Test", null, EntryEventType.REMOVED.getType(), null, oldValue, null);
		this.repository.entryRemoved(entryEvent);
		//No NullPointerException from event.getOldValue().getId()
		verify(this.applicationEventPublisher, times(0)).publishEvent(any(SessionDeletedEvent.class));
	}

}
