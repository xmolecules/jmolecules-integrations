package org.jmolecules.annotation.processor.example;

import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifier;

class MyEntity implements Entity<ValidAggregateRoot, Identifier> {

	ValidAggregateRoot root;

	@Override
	public Identifier getId() {
		return null;
	}
}
