package org.jmolecules.annotation.processor.example;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

public class ValidAggregateRoot implements AggregateRoot<ValidAggregateRoot, Identifier> {

	@Override
	public Identifier getId() {
		return null;
	}
}
