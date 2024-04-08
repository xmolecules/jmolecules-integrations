package example.valid;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
class WithMethodIdentity {

	@Identity
	public void getId() {}
}
