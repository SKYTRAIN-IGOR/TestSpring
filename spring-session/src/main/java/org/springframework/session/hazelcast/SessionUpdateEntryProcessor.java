/*
 * Copyright 2014-2018 the original author or authors.
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

package org.springframework.session.hazelcast;

import java.util.Map;

import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.map.EntryProcessor;

import org.springframework.session.MapSession;

/**
 * Hazelcast {@link EntryProcessor} responsible for handling updates to session.
 *
 * @author Vedran Pavic
 * @since 1.3.4
 * @see HazelcastSessionRepository#save(HazelcastSessionRepository.HazelcastSession)
 */
public class SessionUpdateEntryProcessor
		extends AbstractEntryProcessor<String, MapSession> {

	private long lastAccessedTime;

	private boolean lastAccessedTimeSet;

	private int maxInactiveInterval;

	private boolean maxInactiveIntervalSet;

	private Map<String, Object> delta;

	public Object process(Map.Entry<String, MapSession> entry) {
		MapSession value = entry.getValue();
		if (value == null) {
			return Boolean.FALSE;
		}
		if (this.lastAccessedTimeSet) {
			value.setLastAccessedTime(this.lastAccessedTime);
		}
		if (this.maxInactiveIntervalSet) {
			value.setMaxInactiveIntervalInSeconds(this.maxInactiveInterval);
		}
		if (this.delta != null) {
			for (final Map.Entry<String, Object> attribute : this.delta.entrySet()) {
				if (attribute.getValue() != null) {
					value.setAttribute(attribute.getKey(), attribute.getValue());
				}
				else {
					value.removeAttribute(attribute.getKey());
				}
			}
		}
		entry.setValue(value);
		return Boolean.TRUE;
	}

	void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
		this.lastAccessedTimeSet = true;
	}

	void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
		this.maxInactiveIntervalSet = true;
	}

	void setDelta(Map<String, Object> delta) {
		this.delta = delta;
	}

}
