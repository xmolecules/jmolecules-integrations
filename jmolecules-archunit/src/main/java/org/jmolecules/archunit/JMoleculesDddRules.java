/*
 * Copyright 2020-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmolecules.archunit;

import static com.tngtech.archunit.base.DescribedPredicate.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifiable;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.ddd.types.ValueObject;
import org.springframework.core.ResolvableType;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * A set of ArchUnit rules that allow verification of domain models. In short the rules here verify:
 * <ul>
 * <li>Aggregates only refer to entities that are declared to be part of it.</li>
 * <li>References to other aggregates are established via {@link org.jmolecules.ddd.types.Association}s or identifier
 * references.</li>
 * <li>Annotated identifiables do have an identifier.</li>
 * <li>Value objects and identifiers do not refer to identifiables.</li>
 * </ul>
 * Those rules are mostly driven by what's been presented by John Sullivan in his blog post
 * <a href="http://scabl.blogspot.com/2015/04/aeddd-9.html">here</a>.
 *
 * @author Oliver Drotbohm
 * @author Torsten Juergeleit
 * @author Hasan Kara
 * @see <a href="http://scabl.blogspot.com/2015/04/aeddd-9.html">Advancing Enterprise DDD - Reinstating the Aggregate</a>
 */
public class JMoleculesDddRules {

	private static DescribedPredicate<CanBeAnnotated> IS_ANNOTATED_IDENTIFIABLE = annotatedWith(
			org.jmolecules.ddd.annotation.AggregateRoot.class)
					.or(annotatedWith(org.jmolecules.ddd.annotation.Entity.class));

	private static DescribedPredicate<CanBeAnnotated> IS_ANNOTATED_VALUE_OBJECT = annotatedWith(
			org.jmolecules.ddd.annotation.ValueObject.class);

	private static DescribedPredicate<JavaClass> IS_IMPLEMENTING_IDENTIFIABLE = implement(Identifiable.class);
	private static DescribedPredicate<JavaClass> IS_IMPLEMENTING_VALUE_OBJECT = implement(ValueObject.class);

	private static DescribedPredicate<JavaClass> IS_IDENTIFIABLE = IS_IMPLEMENTING_IDENTIFIABLE
			.or(IS_ANNOTATED_IDENTIFIABLE);
	private static DescribedPredicate<JavaClass> IS_VALUE_OBJECT = IS_IMPLEMENTING_VALUE_OBJECT
			.or(IS_ANNOTATED_VALUE_OBJECT);
	private static DescribedPredicate<JavaClass> IS_IDENTIFIER = implement(Identifier.class);

	/**
	 * An {@link ArchRule} that's composed of all other rules declared in this class.
	 *
	 * @return
	 * @see #entitiesShouldBeDeclaredForUseInSameAggregate()
	 * @see #aggregateReferencesShouldBeViaIdOrAssociation()
	 * @see #annotatedEntitiesAndAggregatesNeedToHaveAnIdentifier()
	 * @see #valueObjectsMustNotReferToIdentifiables()
	 */
	public static ArchRule all() {

		return CompositeArchRule //
				.of(entitiesShouldBeDeclaredForUseInSameAggregate()) //
				.and(aggregateReferencesShouldBeViaIdOrAssociation()) //
				.and(annotatedEntitiesAndAggregatesNeedToHaveAnIdentifier()) //
				.and(valueObjectsMustNotReferToIdentifiables()) //
				.allowEmptyShould(true);
	}

	/**
	 * An {@link ArchRule} that verifies that fields that implement {@link Entity} within a type implementing
	 * {@link AggregateRoot} declare the aggregate type as the owning aggregate.
	 * <p />
	 *
	 * <pre>
	 * class Customer implements AggregateRoot&lt;Customer, CustomerId&gt; { … }
	 * class Address implements Entity&lt;Customer, AddressId&gt; { … }
	 *
	 * class LineItem implements Entity&lt;Order, LineItemId&gt; { … }
	 * class Order implements AggregateRoot&lt;Order, OrderId&gt; {
	 *
	 *   List&lt;LineItem&gt; lineItems; // valid
	 *   Address shippingAddress; // invalid as Address is declared to belong to Customer
	 * }
	 * </pre>
	 *
	 * @return will never be {@literal null}.
	 */
	public static ArchRule entitiesShouldBeDeclaredForUseInSameAggregate() {

		return ArchRuleDefinition.fields() //
				.that(new OwnerMatches(IS_IDENTIFIABLE)) //
				.and(areAssignableTo(Entity.class).and(not(areAssignableTo(AggregateRoot.class)))) //
				.should(beDeclaredToBeUsedWithDeclaringAggregate()) //
				.allowEmptyShould(true); //
	}

	/**
	 * An {@link ArchRule} that ensures that one {@link AggregateRoot} does not reference another via the remote
	 * AggregateRoot type but rather via their identifier type or an explicit {@link Association} type.
	 * <p />
	 *
	 * <pre>
	 * class Customer implements AggregateRoot&lt;Customer, CustomerId&gt; { … }
	 *
	 * class Order implements AggregateRoot&lt;Order, OrderId&gt; {
	 *
	 *   Customer customer; // invalid
	 *   CustomerId customerId; // valid
	 *   Association&lt;Customer&gt; customer; // valid
	 * }
	 * </pre>
	 *
	 * @return will never be {@literal null}.
	 */
	public static ArchRule aggregateReferencesShouldBeViaIdOrAssociation() {

		DescribedPredicate<JavaField> referenceAnAggregateRoot = areAssignableTo(AggregateRoot.class)
				.or(hasFieldTypeAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class))
				.or(hasParameterizedFieldOfTypeAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class));

		return ArchRuleDefinition.fields() //
				.that(new OwnerMatches(IS_IDENTIFIABLE).and(referenceAnAggregateRoot)) //
				.should(new ShouldUseIdReferenceOrAssociation())
				.allowEmptyShould(true);
	}

	/**
	 * Verifies that classes annotated with {@link org.jmolecules.ddd.annotation.AggregateRoot} or
	 * {@link org.jmolecules.ddd.annotation.Entity} declare a field annotated with {@link Identifier}.
	 *
	 * @return will never be {@literal null}.
	 */
	public static ArchRule annotatedEntitiesAndAggregatesNeedToHaveAnIdentifier() {

		return ArchRuleDefinition.classes() //
				.that(IS_ANNOTATED_IDENTIFIABLE) //
				.and().areNotAnnotations() //
				.should(new DeclaresAnnotatedFieldOrMethod(Identity.class)) //
				.allowEmptyShould(true);
	}

	/**
	 * Verifies that value objects and identifiers do not refer to identifiables.
	 *
	 * @return will never be {@literal null}.
	 */
	public static ArchRule valueObjectsMustNotReferToIdentifiables() {

		return ArchRuleDefinition.fields() //
				.that(new OwnerMatches(IS_VALUE_OBJECT.or(IS_IDENTIFIER))) //
				.should(new FieldTypeMustNotMatchCondition(IS_IDENTIFIABLE)) //
				.allowEmptyShould(true);
	}

	private static IsDeclaredToUseTheSameAggregate beDeclaredToBeUsedWithDeclaringAggregate() {
		return new IsDeclaredToUseTheSameAggregate();
	}

	private static IsAssignableTypeField areAssignableTo(Class<?> type) {
		return new IsAssignableTypeField(type);
	}

	private static FieldTypeIsAnnotatedWith hasFieldTypeAnnotatedWith(Class<? extends Annotation> type) {
		return new FieldTypeIsAnnotatedWith(type);
	}

	private static DescribedPredicate<? super JavaField> hasParameterizedFieldOfTypeAnnotatedWith(Class<? extends Annotation> type) {
		return new ParameterizedFieldOfTypeAnnotatedWith(type);
	}

	private static class IsDeclaredToUseTheSameAggregate extends ArchCondition<JavaField> {

		private static final ResolvableType COLLECTION_TYPE = ResolvableType.forClass(Collection.class);
		private static final ResolvableType MAP_TYPE = ResolvableType.forClass(Map.class);

		private IsDeclaredToUseTheSameAggregate() {
			super("belong to aggregate the field is declared in", new Object[] {});
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.lang.ArchCondition#check(java.lang.Object, com.tngtech.archunit.lang.ConditionEvents)
		 */
		@Override
		public void check(JavaField item, ConditionEvents events) {

			Field field = item.reflect();
			ResolvableType type = getActualType(ResolvableType.forField(field));
			ResolvableType expectedAggregateType = type.as(Entity.class).getGeneric(0);
			ResolvableType owningType = ResolvableType.forClass(field.getDeclaringClass());
			ResolvableType owningAggregateType = owningType.as(Entity.class).getGeneric(0);
			boolean entitiesBelongToSameAggregate = expectedAggregateType.isAssignableFrom(owningAggregateType);

			if (owningType.isAssignableFrom(expectedAggregateType) || entitiesBelongToSameAggregate) {

				events.add(SimpleConditionEvent.satisfied(field, "Matches"));

			} else {

				String ownerName = FormatableJavaClass.of(item.getOwner()).getAbbreviatedFullName();

				events.add(SimpleConditionEvent.violated(item,
						String.format("Field %s.%s is of type %s and declared to be used from aggregate %s!",
								ownerName, item.getName(), item.getRawType().getSimpleName(),
								expectedAggregateType.resolve(Object.class).getSimpleName())));
			}
		}

		private static ResolvableType getActualType(ResolvableType type) {

			if (COLLECTION_TYPE.isAssignableFrom(type)) {
				return type.getGeneric(0);
			}

			return MAP_TYPE.isAssignableFrom(type) ? type.getGeneric(1) : type;
		}
	}

	private static class ShouldUseIdReferenceOrAssociation extends ArchCondition<JavaField> {

		public ShouldUseIdReferenceOrAssociation() {
			super("use id reference or Association", new Object[0]);
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.lang.ArchCondition#check(java.lang.Object, com.tngtech.archunit.lang.ConditionEvents)
		 */
		@Override
		public void check(JavaField field, ConditionEvents events) {

			events.add(SimpleConditionEvent.violated(field,
					String.format(
							"Field %s.%s refers to an aggregate root (%s). Rather use an identifier reference or Association!",
							FormatableJavaClass.of(field.getOwner()).getAbbreviatedFullName(), field.getName(),
							FormatableJavaClass.of(field.getRawType()).getAbbreviatedFullName())));
		}
	}

	private static class FieldTypeIsAnnotatedWith extends DescribedPredicate<JavaField> {

		private final DescribedPredicate<CanBeAnnotated> isAnnotatedWith;

		public FieldTypeIsAnnotatedWith(Class<? extends Annotation> type) {

			super("is of type annotated with %s", type.getSimpleName());

			this.isAnnotatedWith = CanBeAnnotated.Predicates.annotatedWith(type);
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(JavaField input) {
			return isAnnotatedWith.test(input.getRawType());
		}
	}

	private static class ParameterizedFieldOfTypeAnnotatedWith extends DescribedPredicate<JavaField> {

		private final DescribedPredicate<CanBeAnnotated> isAnnotatedWith;

		public ParameterizedFieldOfTypeAnnotatedWith(Class<? extends Annotation> type) {

			super("is collection of type annotated with %s", type.getSimpleName());

			this.isAnnotatedWith = CanBeAnnotated.Predicates.annotatedWith(type);
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(JavaField input) {
			if (!(input.getType() instanceof JavaParameterizedType)) {
				return false;
			}
			JavaParameterizedType parameterizedType = (JavaParameterizedType) input.getType();
			return parameterizedType.getActualTypeArguments()
					.stream()
					.map(JavaType::toErasure)
					.anyMatch(isAnnotatedWith);
		}
	}

	private static class IsAssignableTypeField extends DescribedPredicate<JavaField> {

		private static final ResolvableType COLLECTION_TYPE = ResolvableType.forClass(Collection.class);
		private static final ResolvableType MAP_TYPE = ResolvableType.forClass(Map.class);

		private final Class<?> type;

		private IsAssignableTypeField(Class<?> type) {
			super("are assignable to %s", new Object[] { type.getName() });
			this.type = type;
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(JavaField input) {

			ResolvableType fieldType = ResolvableType.forField(input.reflect());
			ResolvableType domainType = unwrapDomainType(fieldType);

			return ResolvableType.forClass(type).isAssignableFrom(domainType);
		}

		private static ResolvableType unwrapDomainType(ResolvableType fieldType) {

			if (COLLECTION_TYPE.isAssignableFrom(fieldType)) {
				return fieldType.as(Collection.class).getGeneric(0);
			}

			if (MAP_TYPE.isAssignableFrom(fieldType)) {
				return fieldType.as(Map.class).getGeneric(1);
			}

			return fieldType;
		}
	}

	private static class DeclaresAnnotatedFieldOrMethod extends ArchCondition<JavaClass> {

		private final Class<? extends Annotation> annotation;

		DeclaresAnnotatedFieldOrMethod(Class<? extends Annotation> annotation) {

			super("declares field or method  (meta-)annotated with %s", annotation.getName());

			this.annotation = annotation;
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.lang.ArchCondition#check(java.lang.Object, com.tngtech.archunit.lang.ConditionEvents)
		 */
		@Override
		public void check(JavaClass input, ConditionEvents events) {

			boolean annotatedFieldDeclared = input.getAllFields().stream()
					.anyMatch(it -> it.isAnnotatedWith(annotation) || it.isMetaAnnotatedWith(annotation));
			boolean annotatedMethodDeclared = input.getAllMethods().stream()
					.anyMatch(it -> it.isAnnotatedWith(annotation) || it.isMetaAnnotatedWith(annotation));

			if (!annotatedFieldDeclared && !annotatedMethodDeclared) {

				String message = String.format("Type %s must declare a field or a method annotated with %s!", //
						FormatableJavaClass.of(input).getAbbreviatedFullName(), annotation.getName());

				events.add(SimpleConditionEvent.violated(input, message));
			}
		}
	}

	private static class OwnerMatches extends DescribedPredicate<JavaField> {

		private final DescribedPredicate<JavaClass> condition;

		public OwnerMatches(DescribedPredicate<JavaClass> condition) {

			super(String.format("are declared in a jMolecules type that %s", condition.getDescription()),
					new Object[0]);

			this.condition = condition;
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(JavaField input) {
			return condition.test(input.getOwner());
		}
	}

	private static class FieldTypeMustNotMatchCondition extends ArchCondition<JavaField> {

		private final DescribedPredicate<JavaClass> condition;

		public FieldTypeMustNotMatchCondition(DescribedPredicate<JavaClass> condition) {

			super(condition.getDescription());

			this.condition = condition;
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.lang.ArchCondition#check(java.lang.Object, com.tngtech.archunit.lang.ConditionEvents)
		 */
		@Override
		public void check(JavaField item, ConditionEvents events) {

			JavaClass type = item.getRawType();

			if (condition.test(type)) {

				String ownerName = FormatableJavaClass.of(item.getOwner()).getAbbreviatedFullName();
				String typeName = FormatableJavaClass.of(item.getRawType()).getAbbreviatedFullName();

				String message = String.format("Field %s.%s refers to identifiable %s", ownerName, item.getName(),
						typeName);

				events.add(SimpleConditionEvent.violated(item, message));
			}
		}
	}
}
