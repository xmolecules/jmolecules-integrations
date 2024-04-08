package example;

import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifier;

import example.valid.ValidAggregateRoot;

class MyEntity implements Entity<ValidAggregateRoot, Identifier> {

	ValidAggregateRoot root;

	@Override
	public Identifier getId() {
		return null;
	}
}
