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
package org.springframework.session.jdbc.config.annotation.web.http;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Spring @Configuration class used to configure and initialize a JDBC based HttpSession
 * provider implementation in Spring Session.
 * <p>
 * Exposes the {@link org.springframework.session.web.http.SessionRepositoryFilter} as a
 * bean named "springSessionRepositoryFilter". In order to use this a single
 * {@link DataSource} must be exposed as a Bean.
 *
 * @author Vedran Pavic
 * @author Eddú Meléndez
 * @since 1.2.0
 * @see EnableJdbcHttpSession
 */
@Configuration
@EnableScheduling
public class JdbcHttpSessionConfiguration extends SpringHttpSessionConfiguration
		implements BeanClassLoaderAware, ImportAware, SchedulingConfigurer {

	private String tableName;

	private Integer maxInactiveIntervalInSeconds;

	private String cleanUpCron;

	private LobHandler lobHandler;

	@Autowired(required = false)
	@Qualifier("conversionService")
	private ConversionService conversionService;

	private ConversionService springSessionConversionService;

	private ClassLoader classLoader;

	@Autowired
	private JdbcOperationsSessionRepository sessionRepository;

	@Bean
	public JdbcTemplate springSessionJdbcOperations(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	public JdbcOperationsSessionRepository sessionRepository(
			@Qualifier("springSessionJdbcOperations") JdbcOperations jdbcOperations,
			PlatformTransactionManager transactionManager) {
		JdbcOperationsSessionRepository sessionRepository =
				new JdbcOperationsSessionRepository(jdbcOperations, transactionManager);
		String tableName = getTableName();
		if (StringUtils.hasText(tableName)) {
			sessionRepository.setTableName(tableName);
		}
		sessionRepository
				.setDefaultMaxInactiveInterval(this.maxInactiveIntervalInSeconds);
		if (this.lobHandler != null) {
			sessionRepository.setLobHandler(this.lobHandler);
		}
		if (this.springSessionConversionService != null) {
			sessionRepository.setConversionService(this.springSessionConversionService);
		}
		else if (this.conversionService != null) {
			sessionRepository.setConversionService(this.conversionService);
		}
		else if (deserializingConverterSupportsCustomClassLoader()) {
			GenericConversionService conversionService = createConversionServiceWithBeanClassLoader();
			sessionRepository.setConversionService(conversionService);
		}
		return sessionRepository;
	}

	@Bean
	public Executor executor() {
		return Executors.newScheduledThreadPool(10);
	}

	/**
	 * This must be a separate method because some ClassLoaders load the entire method
	 * definition even if an if statement guards against it loading. This means that older
	 * versions of Spring would cause a NoSuchMethodError if this were defined in
	 * {@link #sessionRepository(JdbcOperations, PlatformTransactionManager)}.
	 *
	 * @return the default {@link ConversionService}
	 */
	private GenericConversionService createConversionServiceWithBeanClassLoader() {
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(Object.class, byte[].class,
				new SerializingConverter());
		conversionService.addConverter(byte[].class, Object.class,
				new DeserializingConverter(this.classLoader));
		return conversionService;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Autowired(required = false)
	@Qualifier("springSessionLobHandler")
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	@Autowired(required = false)
	@Qualifier("springSessionConversionService")
	public void setSpringSessionConversionService(ConversionService conversionService) {
		this.springSessionConversionService = conversionService;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
		this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
	}

	public void setCleanUpCron(String cleanUpCron) {
		this.cleanUpCron = cleanUpCron;
	}

	private String getTableName() {
		String systemProperty = System.getProperty("spring.session.jdbc.tableName", "");
		if (StringUtils.hasText(systemProperty)) {
			return systemProperty;
		}
		return this.tableName;
	}

	private String getCleanUpCron() {
		String systemProperty = System.getProperty("spring.session.jdbc.cleanUpCron", "");
		if (StringUtils.hasText(systemProperty)) {
			return systemProperty;
		}
		return this.cleanUpCron;
	}

	private boolean deserializingConverterSupportsCustomClassLoader() {
		return ClassUtils.hasConstructor(DeserializingConverter.class, ClassLoader.class);
	}

	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(executor());
		taskRegistrar.addCronTask(new CronTask(new Runnable() {
			public void run() {
				JdbcHttpSessionConfiguration.this.sessionRepository
						.cleanUpExpiredSessions();
			}
		}, getCleanUpCron()));
	}

	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, Object> enableAttrMap = importMetadata
				.getAnnotationAttributes(EnableJdbcHttpSession.class.getName());
		AnnotationAttributes enableAttrs = AnnotationAttributes.fromMap(enableAttrMap);
		this.tableName = enableAttrs.getString("tableName");
		this.maxInactiveIntervalInSeconds = enableAttrs
				.getNumber("maxInactiveIntervalInSeconds");
		this.cleanUpCron = enableAttrs.getString("cleanUpCron");
	}
}
