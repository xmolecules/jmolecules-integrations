package example;

import org.jmolecules.ddd.types.ValueObject;

class MyValueObject implements ValueObject {

	MyEntity entity;
	MyAggregateRoot aggregateRoot;
	AnnotatedAggregateRoot annotatedAggregateRoot;
}
