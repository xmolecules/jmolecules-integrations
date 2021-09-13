/*
 * Copyright 2021 the original author or authors.
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
package org.jmolecules.spring.config;

import java.util.function.Supplier;

import org.jmolecules.spring.AssociationToPrimitivesConverter;
import org.jmolecules.spring.IdentifierToPrimitivesConverter;
import org.jmolecules.spring.PrimitivesToAssociationConverter;
import org.jmolecules.spring.PrimitivesToIdentifierConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.util.Assert;

/**
 * Utilities to easily configure JMolecules {@link Converter} implementations within a {@link ConversionService}.
 *
 * @author Oliver Drotbohm
 */
public class JMoleculesConverterConfigUtils {

	/**
	 * Registers the following {@link Converter} instances in the given {@link ConversionService}:
	 * <ul>
	 * <li>{@link PrimitivesToIdentifierConverter}</li>
	 * <li>{@link IdentifierToPrimitivesConverter}</li>
	 * <li>{@link PrimitivesToAssociationConverter}</li>
	 * <li>{@link AssociationToPrimitivesConverter}</li>
	 * </ul>
	 *
	 * @param <T> the concrete {@link ConfigurableConversionService} subtype
	 * @param service must not be {@literal null}.
	 * @return
	 */
	public static <T extends ConfigurableConversionService> T registerConverters(T service) {

		Assert.notNull(service, "ConfigurableConversionService must not be null!");

		Supplier<ConversionService> supplier = () -> service;

		IdentifierToPrimitivesConverter identifierToPrimitives = new IdentifierToPrimitivesConverter(supplier);
		PrimitivesToIdentifierConverter primitivesToIdentifier = new PrimitivesToIdentifierConverter(supplier);

		service.addConverter(identifierToPrimitives);
		service.addConverter(primitivesToIdentifier);
		service.addConverter(new PrimitivesToAssociationConverter<>(primitivesToIdentifier));
		service.addConverter(new AssociationToPrimitivesConverter<>(identifierToPrimitives));

		return service;
	}
}
