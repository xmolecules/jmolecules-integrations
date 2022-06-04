/*
 * Copyright 2021 the original author or authors.
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
package org.jmolecules.spring.jdbc;

import org.jmolecules.spring.data.MutablePersistable;
import org.springframework.data.relational.core.mapping.event.AfterConvertCallback;
import org.springframework.data.relational.core.mapping.event.AfterSaveCallback;

/**
 * Spring Data JDBC entity callback to flip the is-new flag for just persisted and loaded entities.
 *
 * @author Oliver Drotbohm
 */
public class NotNewCallback<T extends MutablePersistable<T, ?>>
		implements AfterConvertCallback<T>, AfterSaveCallback<T> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.mapping.event.AfterConvertCallback#onAfterConvert(java.lang.Object)
	 */
	@Override
	public T onAfterConvert(T aggregate) {

		aggregate.__jMolecules__markNotNew();

		return aggregate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.mapping.event.AfterSaveCallback#onAfterSave(java.lang.Object)
	 */
	@Override
	public T onAfterSave(T aggregate) {

		if (aggregate.isNew()) {
			aggregate.__jMolecules__markNotNew();
		}

		return aggregate;
	}
}
