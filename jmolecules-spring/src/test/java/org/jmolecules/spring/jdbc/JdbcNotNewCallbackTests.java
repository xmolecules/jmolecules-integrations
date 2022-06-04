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

import static org.assertj.core.api.Assertions.*;

import org.jmolecules.spring.SamplePersistable;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NotNewCallback}.
 *
 * @author Oliver Drotbohm
 */
class JdbcNotNewCallbackTests {

	@Test
	void marksEntityAsNotNewAfterLoad() {

		NotNewCallback<SamplePersistable> callback = new NotNewCallback<>();

		assertThat(callback.onAfterConvert(new SamplePersistable()).isNew()).isFalse();
	}

	@Test
	void marksEntityAsNotNewAfterSave() {

		NotNewCallback<SamplePersistable> callback = new NotNewCallback<>();

		SamplePersistable sample = callback.onAfterSave(new SamplePersistable());

		assertThat(sample.isNew()).isFalse();
		assertThat(callback.onAfterSave(sample).isNew()).isFalse();
	}
}
