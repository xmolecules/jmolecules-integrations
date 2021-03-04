/*
 * Copyright 2020 the original author or authors.
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
package org.jmolecules.examples.mongodb;

import static org.assertj.core.api.Assertions.*;

import lombok.RequiredArgsConstructor;

import org.jmolecules.examples.mongodb.customer.Address;
import org.jmolecules.examples.mongodb.customer.Customer;
import org.jmolecules.examples.mongodb.customer.CustomerManagement;
import org.jmolecules.examples.mongodb.customer.Customers;
import org.jmolecules.examples.mongodb.order.Order;
import org.jmolecules.examples.mongodb.order.Orders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * @author Oliver Drotbohm
 */
@SpringBootTest
@RequiredArgsConstructor
class ApplicationIntegrationTests {

	private final ConfigurableApplicationContext context;

	@Test
	void bootstrapsContainer() {

		assertThat(AssertableApplicationContext.get(() -> context)) //
				.hasSingleBean(CustomerManagement.class)
				.satisfies(ctx -> {

					ctx.publishEvent(new CustomerManagement.SampleEvent());

					CustomerManagement bean = ctx.getBean(CustomerManagement.class);

					assertThat(bean.eventReceived).isTrue();
				});
	}

	@Test
	void exposesAssociationInMetamodel() {

		var mapping = context.getBean(MongoMappingContext.class);
		var entity = mapping.getRequiredPersistentEntity(Order.class);
		var customer = entity.getRequiredPersistentProperty("customer");

		assertThat(customer.isAssociation()).isTrue();
	}

	@Test
	void persistsDomainModel() {

		var address = new Address("41 Greystreet", "Dreaming Tree", "2731");

		var customers = context.getBean(Customers.class);
		var customer = customers.save(new Customer("Dave", "Matthews", address));

		var orders = context.getBean(Orders.class);

		Order order = new Order(customer)
				.addLineItem("Foo")
				.addLineItem("Bar");

		var result = orders.save(order);

		assertThat(customers.resolveRequired(result.getCustomer()));
	}
}
