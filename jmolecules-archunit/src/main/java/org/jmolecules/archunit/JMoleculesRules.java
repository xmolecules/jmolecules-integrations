/*
 * Copyright 2020-2025 the original author or authors.
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

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Entity;

import com.tngtech.archunit.lang.ArchRule;

/**
 * A set of ArchUnit rules that allow verification of domain models. In short the rules here verify:
 * <ul>
 * <li>Aggregates only refer to entities that are declared to be part of it.</li>
 * <li>References to other aggregates are established via {@link Association}s or identifier references.</li>
 * </ul>
 * Those rules are mostly driven by what's been presented by John Sullivan in his blog post
 * <a href="http://scabl.blogspot.com/2015/04/aeddd-9.html">here</a>.
 *
 * @author Oliver Drotbohm
 * @see <a href="http://scabl.blogspot.com/2015/04/aeddd-9.html">Advancing Enterprise DDD - Reinstating the Aggregate</a>
 * @deprecated since 0.5, for removal in 0.6. Use {@link JMoleculesDddRules} instead.
 */
@Deprecated
public class JMoleculesRules {

	/**
	 * An {@link ArchRule} that's composed of all other rules declared in this class.
	 *
	 * @return
	 * @see #entitiesShouldBeDeclaredForUseInSameAggregate()
	 * @see #aggregateReferencesShouldBeViaIdOrAssociation()
	 */
	public static ArchRule all() {
		return org.jmolecules.archunit.JMoleculesDddRules.all();
	}

	/**
	 * An {@link ArchRule} that verifies that fields that implement {@link Entity} within a type implementing
	 * {@link AggregateRoot} declare the aggregate type as the owning aggregate.
	 * <p />
	 * <code>
	 * class Customer implements AggregateRoot<Customer, CustomerId> { … }
	 * class Address implements Entity<Customer, AddressId> { … }
	 *
	 * class LineItem implements Entity<Order, LineItemId> { … }
	 * class Order implements AggregateRoot<Order, OrderId> {
	 *
	 *   List<LineItem> lineItems; // valid
	 *   Address shippingAddress; // invalid as Address is declared to belong to Customer
	 * }
	 * </code>
	 *
	 * @return will never be {@literal null}.
	 */
	public static ArchRule entitiesShouldBeDeclaredForUseInSameAggregate() {
		return org.jmolecules.archunit.JMoleculesDddRules.entitiesShouldBeDeclaredForUseInSameAggregate();
	}

	/**
	 * An {@link ArchRule} that ensures that one {@link AggregateRoot} does not reference another via the remote
	 * AggregateRoot type but rather via their identifier type or an explicit {@link Association} type.
	 * <p />
	 * <code>
	 * class Customer implements AggregateRoot<Customer, CustomerId> { … }
	 *
	 * class Order implements AggregateRoot<Order, OrderId> {
	 *
	 *   Customer customer; // invalid
	 *   CustomerId customerId; // valid
	 *   Association<Customer> customer; // valid
	 * }
	 * </code>
	 *
	 * @return will never be {@literal null}.
	 */
	public static ArchRule aggregateReferencesShouldBeViaIdOrAssociation() {
		return org.jmolecules.archunit.JMoleculesDddRules.aggregateReferencesShouldBeViaIdOrAssociation();
	}
}
