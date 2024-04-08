package example;

import org.jmolecules.ddd.types.ValueObject;

record MyRecord(MyEntity entity) implements ValueObject {}
