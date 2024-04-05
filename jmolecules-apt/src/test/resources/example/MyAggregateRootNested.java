package example;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

class MyAggregateRootNested {
	
	static class MyAggregateRoot implements AggregateRoot<MyAggregateRoot, Identifier> {

		MyAggregateRoot aggregate;
		String foo;

		@Override
		public Identifier getId() {
			return null;
		}
	}
}
