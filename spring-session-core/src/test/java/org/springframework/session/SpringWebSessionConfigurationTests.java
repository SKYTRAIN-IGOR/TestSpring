/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.session;

import org.junit.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.HeaderWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verify various configurations through {@link EnableSpringWebSession}.
 *
 * @author Greg Turnquist
 */
public class SpringWebSessionConfigurationTests {

	@Test
	public void enableSpringWebSessionConfiguresThings() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(GoodConfig.class);
		ctx.refresh();

		WebSessionManager webSessionManagerFoundByType = ctx.getBean(WebSessionManager.class);
		Object webSessionManagerFoundByName = ctx.getBean(WebHttpHandlerBuilder.WEB_SESSION_MANAGER_BEAN_NAME);

		assertThat(webSessionManagerFoundByType).isNotNull();
		assertThat(webSessionManagerFoundByName).isNotNull();
		assertThat(webSessionManagerFoundByType).isEqualTo(webSessionManagerFoundByName);

		assertThat(ctx.getBean(ReactorSessionRepository.class)).isNotNull();
	}

	@Test
	public void missingReactorSessionRepositoryBreaksAppContext() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BadConfig.class);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(ctx::refresh)
				.withMessageContaining("Error creating bean with name 'webSessionManager'")
				.withMessageContaining("No qualifying bean of type '" + ReactorSessionRepository.class.getCanonicalName());
	}

	@Test
	public void defaultSessionIdResolverShouldBeCookieBased() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(GoodConfig.class);
		ctx.refresh();

		DefaultWebSessionManager manager = ctx.getBean(DefaultWebSessionManager.class);
		assertThat(manager.getSessionIdResolver().getClass()).isAssignableFrom(CookieWebSessionIdResolver.class);
	}

	@Test
	public void providedSessionIdResolverShouldBePickedUpAutomatically() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(OverrideSessionIdResolver.class);
		ctx.refresh();

		DefaultWebSessionManager manager = ctx.getBean(DefaultWebSessionManager.class);
		assertThat(manager.getSessionIdResolver().getClass()).isAssignableFrom(HeaderWebSessionIdResolver.class);
	}

	/**
	 * A configuration with all the right parts.
	 */
	@EnableSpringWebSession
	static class GoodConfig {

		/**
		 * Use Reactor-friendly, {@link java.util.Map}-backed {@link ReactorSessionRepository} for test purposes.
		 */
		@Bean
		ReactorSessionRepository<?> reactorSessionRepository() {
			return new MapReactorSessionRepository();
		}
	}

	/**
	 * A configuration where no {@link ReactorSessionRepository} is defined. It's BAD!
	 */
	@EnableSpringWebSession
	static class BadConfig {

	}

	@EnableSpringWebSession
	static class OverrideSessionIdResolver {

		@Bean
		ReactorSessionRepository<?> reactorSessionRepository() {
			return new MapReactorSessionRepository();
		}

		@Bean
		WebSessionIdResolver alternateWebSessionIdResolver() {
			return new HeaderWebSessionIdResolver();
		}
	}
}
