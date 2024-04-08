package example;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
class AnnotatedValueObject {

	MyEntity entity;
	MyAggregateRoot aggregateRoot;
	AnnotatedAggregateRoot annotatedAggregateRoot;
}
