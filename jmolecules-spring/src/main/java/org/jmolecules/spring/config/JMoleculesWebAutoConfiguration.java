/*
 * Copyright 2021-2024 the original author or authors.
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

import org.jmolecules.spring.AssociationToPrimitivesConverter;
import org.jmolecules.spring.IdentifierToPrimitivesConverter;
import org.jmolecules.spring.PrimitivesToAssociationConverter;
import org.jmolecules.spring.PrimitivesToIdentifierConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration to register the following converters with Spring MVC.
 * <ul>
 * <li>{@link PrimitivesToIdentifierConverter}</li>
 * <li>{@link IdentifierToPrimitivesConverter}</li>
 * <li>{@link PrimitivesToAssociationConverter}</li>
 * <li>{@link AssociationToPrimitivesConverter}</li>
 * </ul>
 *
 * @author Oliver Drotbohm
 * @see IdentifierToPrimitivesConverter
 * @see PrimitivesToIdentifierConverter
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
class JMoleculesWebAutoConfiguration {

	@Bean
	WebMvcConfigurer jMoleculesWebMvcConfigurer() {

		return new WebMvcConfigurer() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer#addFormatters(org.springframework.format.FormatterRegistry)
			 */
			@Override
			public void addFormatters(FormatterRegistry registry) {

				if (!(registry instanceof FormattingConversionService)) {
					return;
				}

				JMoleculesConverterConfigUtils.registerConverters((FormattingConversionService) registry);
			}
		};
	}
}
