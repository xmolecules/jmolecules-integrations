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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Supplier;

import org.jmolecules.ddd.types.Association;
import org.jmolecules.spring.AssociationToPrimitivesConverter;
import org.jmolecules.spring.PrimitivesToAssociationConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Auto-configuration Jackson serializers that automatically render {@link Association} instances as their primitive
 * identifier value and deserialize them back properly.
 *
 * @author Oliver Drotbohm
 * @see AssociationToPrimitivesConverter
 * @see PrimitivesToAssociationConverter
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ObjectMapper.class)
class JMoleculesSpringJacksonAutoConfiguration {

	@Bean
	AssociationResolvingJacksonModule associationResolvingJacksonModule(BeanFactory beanFactory) {

		Supplier<ConversionService> conversionService = () -> beanFactory.containsBean("mvcConversionService")
				? beanFactory.getBean("mvcConversionService", ConversionService.class)
				: beanFactory.getBeanProvider(ConversionService.class).getIfAvailable(() -> {
					return JMoleculesConverterConfigUtils.registerConverters(new DefaultFormattingConversionService());
				});

		return new AssociationResolvingJacksonModule(conversionService);
	}

	static class AssociationResolvingJacksonModule extends SimpleModule {

		private static final long serialVersionUID = 919622286584067203L;

		public AssociationResolvingJacksonModule(Supplier<ConversionService> conversionService) {

			super("jmolecules-association-resolving-module");

			addDeserializer(Association.class, new AssociationDeserializer(conversionService));
			addSerializer(new AssociationSerializer(conversionService));
		}

		private static class AssociationSerializer extends StdSerializer<Association<?, ?>> {

			private static final long serialVersionUID = 610509466313298390L;

			private final Supplier<ConversionService> conversionService;

			AssociationSerializer(Supplier<ConversionService> conversionService) {
				super(Association.class, true);
				this.conversionService = conversionService;
			}

			/*
			 * (non-Javadoc)
			 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
			 */
			@Override
			public void serialize(Association<?, ?> value, JsonGenerator gen, SerializerProvider provider)
					throws IOException {

				if (value == null) {
					gen.writeNull();
					return;
				}

				Object primitive = conversionService.get().convert(value, Object.class);

				provider.findValueSerializer(primitive.getClass()).serialize(primitive, gen, provider);
			}
		}

		private static class AssociationDeserializer extends StdDeserializer<Association<?, ?>>
				implements ContextualDeserializer {

			private static final long serialVersionUID = -3979882455489371634L;
			private final Supplier<ConversionService> converter;
			private final BeanProperty property;

			public AssociationDeserializer(Supplier<ConversionService> converter) {
				this(converter, null);
			}

			private AssociationDeserializer(Supplier<ConversionService> converter, BeanProperty property) {

				super(Association.class);

				this.converter = converter;
				this.property = property;
			}

			/*
			 * (non-Javadoc)
			 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
			 */
			@Override
			public Association<?, ?> deserialize(JsonParser p, DeserializationContext ctxt)
					throws IOException, JsonProcessingException {

				String source = p.getText();

				if (!StringUtils.hasText(source)) {
					return null;
				}

				List<JavaType> parameters = property.getType().getBindings().getTypeParameters();
				ResolvableType associationType = ResolvableType.forClassWithGenerics(Association.class,
						parameters.get(0).getRawClass(), parameters.get(1).getRawClass());

				return (Association<?, ?>) converter.get().convert(p.getText(), TypeDescriptor.valueOf(String.class),
						new TypeDescriptor(associationType, Association.class, new Annotation[0]));
			}

			/*
			 * (non-Javadoc)
			 * @see com.fasterxml.jackson.databind.deser.ContextualDeserializer#createContextual(com.fasterxml.jackson.databind.DeserializationContext, com.fasterxml.jackson.databind.BeanProperty)
			 */
			@Override
			public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
					throws JsonMappingException {

				return new AssociationDeserializer(converter, property);
			}
		}
	}
}
