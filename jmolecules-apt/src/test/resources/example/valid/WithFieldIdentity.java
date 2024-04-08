package example.valid;

import java.util.UUID;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
class WithFieldIdentity {
	@Identity UUID id;
}
