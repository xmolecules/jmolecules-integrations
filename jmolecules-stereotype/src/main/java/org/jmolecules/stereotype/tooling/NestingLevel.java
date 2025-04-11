/*
 * Copyright 2025 the original author or authors.
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
package org.jmolecules.stereotype.tooling;

import lombok.Value;

/**
 * A nesting level.
 *
 * @author Oliver Drotbohm
 */
@Value
public class NestingLevel {

	public static final NestingLevel ROOT = new NestingLevel(0);

	private final int level;

	/**
	 * Creates a new {@link NestingLevel} for the given level.
	 *
	 * @param level must not be negative.
	 */
	private NestingLevel(int level) {

		if (level < 0) {
			throw new IllegalArgumentException("Level must not be negative!");
		}

		this.level = level;
	}

	/**
	 * Returns a new, increased {@link NestingLevel}.
	 *
	 * @return will never be {@literal null}.
	 */
	public NestingLevel increase() {
		return new NestingLevel(level + 1);
	}

	/**
	 * Returns a new decreased {@link NestingLevel}.
	 *
	 * @return will never be {@literal null}.
	 */
	public NestingLevel decrease() {
		return new NestingLevel(level - 1);
	}

	boolean isRoot() {
		return level == 0;
	}

	/**
	 * Returns the current level.
	 */
	public int getLevel() {
		return level;
	}
}
