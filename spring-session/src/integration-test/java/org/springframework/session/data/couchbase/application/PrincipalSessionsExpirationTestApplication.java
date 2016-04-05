/*
 * Copyright 2014-2016 the original author or authors.
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
package org.springframework.session.data.couchbase.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.couchbase.CouchbaseDao;
import org.springframework.session.data.couchbase.Serializer;
import org.springframework.session.data.couchbase.application.content.ImmediateSessionExpirationRepository;
import org.springframework.session.data.couchbase.application.content.IntegrationTestApplication;
import org.springframework.session.data.couchbase.config.annotation.web.http.EnableCouchbaseHttpSession;

/**
 * Application for testing principal HTTP sessions expiration.
 *
 * @author Mariusz Kopylec
 * @since 1.2.0
 */
@IntegrationTestApplication
@EnableCouchbaseHttpSession(namespace = PrincipalSessionsExpirationTestApplication.HTTP_SESSION_NAMESPACE, timeoutInSeconds = PrincipalSessionsExpirationTestApplication.SESSION_TIMEOUT, principalSessionsEnabled = PrincipalSessionsExpirationTestApplication.PRINCIPAL_SESSIONS_ENABLED)
public class PrincipalSessionsExpirationTestApplication {
	/**
	 * HTTP session application namespace name.
	 */
	public static final String HTTP_SESSION_NAMESPACE = "test-application";
	/**
	 * HTTP session maximum inactive interval in seconds.
	 */
	public static final int SESSION_TIMEOUT = 1;
	/**
	 * Principal HTTP sessions enabling flag.
	 */
	public static final boolean PRINCIPAL_SESSIONS_ENABLED = true;

	@Bean
	@Primary
	public SessionRepository sessionRepository(CouchbaseDao dao, ObjectMapper mapper,
			Serializer serializer) {
		return new ImmediateSessionExpirationRepository(dao, HTTP_SESSION_NAMESPACE,
				mapper, SESSION_TIMEOUT, serializer, PRINCIPAL_SESSIONS_ENABLED);
	}

	public static void main(String[] args) {
		SpringApplication.run(PrincipalSessionsExpirationTestApplication.class, args);
	}
}
