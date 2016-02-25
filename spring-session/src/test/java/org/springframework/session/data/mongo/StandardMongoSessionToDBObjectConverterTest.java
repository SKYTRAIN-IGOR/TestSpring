/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.session.data.mongo;

import com.mongodb.DBObject;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.session.FindByIndexNameSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jakub Kubrynski
 */
public class StandardMongoSessionToDBObjectConverterTest {

	@Test
	public void shouldExtractPrincipalNameFromAttributes() throws Exception {
		//given
		StandardMongoSessionToDBObjectConverter sut = new StandardMongoSessionToDBObjectConverter();
		MongoExpiringSession toSerialize = new MongoExpiringSession();
		String principalName = "john_the_springer";
		toSerialize.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principalName);

		//when
		DBObject dbObject = sut.convert(toSerialize);

		//then
		assertThat(dbObject.get("principal")).isEqualTo(principalName);
	}

	@Test
	public void shouldExtractPrincipalNameFromAuthentication() throws Exception {
		//given
		StandardMongoSessionToDBObjectConverter sut = new StandardMongoSessionToDBObjectConverter();
		MongoExpiringSession toSerialize = new MongoExpiringSession();
		String principalName = "john_the_springer";
		SecurityContextImpl context = new SecurityContextImpl();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(principalName, null));
		toSerialize.setAttribute("SPRING_SECURITY_CONTEXT", context);

		//when
		DBObject dbObject = sut.convert(toSerialize);

		//then
		assertThat(dbObject.get("principal")).isEqualTo(principalName);
	}
}