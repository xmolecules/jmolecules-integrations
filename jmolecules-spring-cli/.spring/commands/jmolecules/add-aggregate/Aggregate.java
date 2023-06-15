package {{root-package}}.{{module}};

import java.util.UUID;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import {{root-package}}.{{module}}.{{capitalizeFirst name}}.{{capitalizeFirst name}}Identifier;


/**
 * A {{capitalizeFirst name}}.
 */
public class {{capitalizeFirst name}} implements AggregateRoot<{{capitalizeFirst name}}, {{capitalizeFirst name}}Identifier> {

	private final {{capitalizeFirst name}}Identifier id;

	public {{capitalizeFirst name}}() {
		this.id = new {{capitalizeFirst name}}Identifier(UUID.randomUUID());
	}

	@Override
	public {{capitalizeFirst name}}Identifier getId() {
		return id;
	}

	record {{capitalizeFirst name}}Identifier(UUID id) implements Identifier {}
}
