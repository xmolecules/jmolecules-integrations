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
package org.jmolecules.spring.mongodb;

import org.bson.Document;
import org.jmolecules.spring.data.MutablePersistable;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveCallback;

/**
 * @author Oliver Drotbohm
 */
public class NotNewCallback<T extends MutablePersistable<T, ?>>
		implements AfterConvertCallback<T>, AfterSaveCallback<T> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback#onAfterConvert(java.lang.Object, org.bson.Document, java.lang.String)
	 */
	@Override
	public T onAfterConvert(T entity, Document document, String collection) {

		entity.__jMolecules__markNotNew();

		return entity;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.mapping.event.AfterSaveCallback#onAfterSave(java.lang.Object, org.bson.Document, java.lang.String)
	 */
	@Override
	public T onAfterSave(T entity, Document document, String collection) {

		if (entity.isNew()) {
			entity.__jMolecules__markNotNew();
		}

		return entity;
	}
}
