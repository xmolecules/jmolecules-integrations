package example.valid;

import example.MyAggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.ddd.types.ValueObject;

import java.util.UUID;

class ValidValueObject implements ValueObject {
	UUID id;
	Association<MyAggregateRoot, Identifier> association;
}
