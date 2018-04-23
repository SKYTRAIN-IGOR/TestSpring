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

package org.springframework.session.jdbc.config.annotation.web.http;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.session.MapSession;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSourceReadOnly;
import org.springframework.session.jdbc.config.annotation.SpringSessionPlatformTransactionManager;
import org.springframework.session.jdbc.config.annotation.SpringSessionPlatformTransactionManagerReadOnly;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Spring {@code @Configuration} class used to configure and initialize a JDBC based
 * {@code HttpSession} provider implementation in Spring Session.
 * <p>
 * Exposes the {@link SessionRepositoryFilter} as a bean named
 * {@code springSessionRepositoryFilter}. In order to use this a single {@link DataSource}
 * must be exposed as a Bean.
 *
 * @author Vedran Pavic
 * @author Eddú Meléndez
 * @since 1.2.0
 * @see EnableJdbcHttpSession
 */
@Configuration
@EnableScheduling
public class JdbcHttpSessionConfiguration extends SpringHttpSessionConfiguration
		implements BeanClassLoaderAware, EmbeddedValueResolverAware, ImportAware,
		SchedulingConfigurer {

	static final String DEFAULT_CLEANUP_CRON = "0 * * * * *";

	private Integer maxInactiveIntervalInSeconds = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

	private String tableName = JdbcOperationsSessionRepository.DEFAULT_TABLE_NAME;

	private String cleanupCron = DEFAULT_CLEANUP_CRON;

	private DataSource dataSource;

	private DataSource dataSourceReadOnly;

	private PlatformTransactionManager transactionManager;

	private PlatformTransactionManager transactionManagerReadOnly;

	private LobHandler lobHandler;

	private ConversionService springSessionConversionService;

	private ConversionService conversionService;

	private ClassLoader classLoader;

	private StringValueResolver embeddedValueResolver;

	@Bean
	public JdbcOperationsSessionRepository sessionRepository() {
		JdbcTemplate jdbcTemplate = createJdbcTemplate(this.dataSource);
		JdbcOperationsSessionRepository sessionRepository;
		if (this.dataSourceReadOnly != null && this.transactionManagerReadOnly != null) {
			JdbcTemplate jdbcTemplateReadOnly = createJdbcTemplate(this.dataSourceReadOnly);
			sessionRepository = new JdbcOperationsSessionRepository(
					jdbcTemplate, jdbcTemplateReadOnly, this.transactionManager, this.transactionManagerReadOnly);
		}
		else {
			sessionRepository = new JdbcOperationsSessionRepository(
					jdbcTemplate, this.transactionManager);
		}
		if (StringUtils.hasText(this.tableName)) {
			sessionRepository.setTableName(this.tableName);
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
		else {
			sessionRepository
					.setConversionService(createConversionServiceWithBeanClassLoader());
		}
		return sessionRepository;
	}

	public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
		this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setCleanupCron(String cleanupCron) {
		this.cleanupCron = cleanupCron;
	}

	@Autowired
	public void setDataSource(
			@SpringSessionDataSource ObjectProvider<DataSource> springSessionDataSource,
			ObjectProvider<DataSource> dataSource) {
		DataSource dataSourceToUse = springSessionDataSource.getIfAvailable();
		if (dataSourceToUse == null) {
			dataSourceToUse = dataSource.getObject();
		}
		this.dataSource = dataSourceToUse;
	}

	@Autowired
	public void setDataSourceReadOnly(
			@SpringSessionDataSourceReadOnly ObjectProvider<DataSource> springSessionDataSourceReadOnly) {
		this.dataSourceReadOnly = springSessionDataSourceReadOnly.getIfAvailable();
	}

	@Autowired
	public void setTransactionManager(
			@SpringSessionPlatformTransactionManager ObjectProvider<PlatformTransactionManager> springSessionTransactionManager,
			ObjectProvider<PlatformTransactionManager> transactionManager) {
		PlatformTransactionManager platformTransactionManager = springSessionTransactionManager.getIfAvailable();
		if (platformTransactionManager == null) {
			platformTransactionManager = transactionManager.getObject();
		}
		this.transactionManager = platformTransactionManager;
	}

	@Autowired
	public void setTransactionManagerReadOnly(
			@SpringSessionPlatformTransactionManagerReadOnly ObjectProvider<PlatformTransactionManager> transactionManagerReadOnly) {
		this.transactionManagerReadOnly = transactionManagerReadOnly.getIfAvailable();
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

	@Autowired(required = false)
	@Qualifier("conversionService")
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, Object> attributeMap = importMetadata
				.getAnnotationAttributes(EnableJdbcHttpSession.class.getName());
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributeMap);
		this.maxInactiveIntervalInSeconds = attributes
				.getNumber("maxInactiveIntervalInSeconds");
		String tableNameValue = attributes.getString("tableName");
		if (StringUtils.hasText(tableNameValue)) {
			this.tableName = this.embeddedValueResolver
					.resolveStringValue(tableNameValue);
		}
		String cleanupCron = attributes.getString("cleanupCron");
		if (StringUtils.hasText(cleanupCron)) {
			this.cleanupCron = cleanupCron;
		}
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addCronTask(() -> sessionRepository().cleanUpExpiredSessions(),
				this.cleanupCron);
	}

	private static JdbcTemplate createJdbcTemplate(DataSource dataSource) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.afterPropertiesSet();
		return jdbcTemplate;
	}

	private GenericConversionService createConversionServiceWithBeanClassLoader() {
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(Object.class, byte[].class,
				new SerializingConverter());
		conversionService.addConverter(byte[].class, Object.class,
				new DeserializingConverter(this.classLoader));
		return conversionService;
	}

}
