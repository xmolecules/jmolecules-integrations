/*
 * Copyright 2021-2024 the original author or authors.
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
package org.jmolecules.spring;

import org.jmolecules.spring.data.MutablePersistable;

public class SamplePersistable implements MutablePersistable<SamplePersistable, Object> {

	private Object id;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Persistable#getId()
	 */
	@Override
	public Object getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Persistable#isNew()
	 */
	@Override
	public boolean isNew() {
		return id == null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.spring.jdbc.MutablePersistable#__jMolecules__markNotNew()
	 */
	@Override
	public void __jMolecules__markNotNew() {
		this.id = new Object();
	}
}
