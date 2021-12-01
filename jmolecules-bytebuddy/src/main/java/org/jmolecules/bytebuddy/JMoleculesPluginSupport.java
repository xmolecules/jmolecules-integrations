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
package org.jmolecules.bytebuddy;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.Plugin.WithPreprocessor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.IOException;

/**
 * Simple base class for {@link Plugin} implementations to not have to override the {@link #close()} method all the
 * time.
 *
 * @author Oliver Drotbohm
 */
public abstract class JMoleculesPluginSupport implements WithPreprocessor {

	protected ClassFileLocator locator;

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin.WithPreprocessor#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public void onPreprocess(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
		this.locator = classFileLocator;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {}
}
