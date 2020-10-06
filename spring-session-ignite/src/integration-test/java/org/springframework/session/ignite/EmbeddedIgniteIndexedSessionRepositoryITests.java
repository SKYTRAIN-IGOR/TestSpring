/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.session.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ignite.config.annotation.web.http.EnableIgniteHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Integration tests for {@link IgniteIndexedSessionRepository} using embedded topology.
 *
 * @author Semyon Danilov
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class EmbeddedIgniteIndexedSessionRepositoryITests extends AbstractIgniteIndexedSessionRepositoryITests {

	@BeforeAll
	static void setUpClass() {
		Ignition.stopAll(true);
	}

	@AfterAll
	static void tearDownClass() {
		Ignition.stopAll(true);
	}

	@EnableIgniteHttpSession
	@Configuration
	static class IgniteSessionConfig {

		@Bean
		Ignite ignite() {
			return IgniteITestUtils.embeddedIgniteServer();
		}

	}

}
