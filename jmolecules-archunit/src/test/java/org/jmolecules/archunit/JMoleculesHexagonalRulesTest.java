/*
 * Copyright 2022-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.jmolecules.archunit.JMoleculesArchitectureRules.*;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.architecture.hexagonal.Hexagon.Exposure;
import org.jmolecules.architecture.hexagonal.Hexagon.Referrer;
import org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit;
import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jmolecules.archunit.JMoleculesArchitectureRules.VerificationDepth;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * Unit tests for {@link JMoleculesArchitectureRules#ensureHexagonal()} and
 * {@link JMoleculesArchitectureRules#ensureHexagonalStrict()}.
 *
 * @author Oliver Drotbohm
 */
@SuppressWarnings("removal") // also covers the deprecated entry points until they are removed
class JMoleculesHexagonalRulesTest {

	private static final String PREFIX = "Field <org.jmolecules.archunit.hexagonal.units.";

	@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.pkg")
	static class PackageBased {

		List<String> expected = Arrays.asList("adapter.SampleAdapter.application", //
				"adapter.SamplePrimaryAdapter.application", //
				"adapter.SamplePrimaryAdapter.secondaryAdapter", //
				"adapter.SamplePrimaryAdapter.secondaryAdapterPackage", //
				"adapter.SamplePrimaryAdapter.secondaryPort", //
				"adapter.SamplePrimaryAdapter.secondaryPortPackage", //
				"adapter.SampleSecondaryAdapter.application", //
				"adapter.SampleSecondaryAdapter.primaryAdapter", //
				"adapter.SampleSecondaryAdapter.primaryAdapterPackage", //
				"adapter.SampleSecondaryAdapter.primaryPort", //
				"adapter.SampleSecondaryAdapter.primaryPortPackage", //
				"adapter.primary.SamplePrimaryAdapterPackage.application", //
				"adapter.primary.SamplePrimaryAdapterPackage.secondaryAdapter", //
				"adapter.primary.SamplePrimaryAdapterPackage.secondaryAdapterPackage", //
				"adapter.primary.SamplePrimaryAdapterPackage.secondaryPort", //
				"adapter.primary.SamplePrimaryAdapterPackage.secondaryPortPackage", //
				"adapter.secondary.SampleSecondaryAdapterPackage.application", //
				"adapter.secondary.SampleSecondaryAdapterPackage.primaryAdapter", //
				"adapter.secondary.SampleSecondaryAdapterPackage.primaryAdapterPackage", //
				"adapter.secondary.SampleSecondaryAdapterPackage.primaryPort", //
				"adapter.secondary.SampleSecondaryAdapterPackage.primaryPortPackage", //
				"application.SampleApplication.adapter", //
				"application.SampleApplication.primaryAdapter", //
				"application.SampleApplication.primaryAdapterPackage", //
				"application.SampleApplication.secondaryAdapter", //
				"application.SampleApplication.secondaryAdapterPackage", //
				"port.SamplePort.adapter()", //
				"port.SamplePort.primaryAdapter()", //
				"port.SamplePort.primaryAdapterPackage()", //
				"port.SamplePort.secondaryAdapter()", //
				"port.SamplePort.secondaryAdapterPackage()", //
				"port.SamplePrimaryPort.adapter()", //
				"port.SamplePrimaryPort.primaryAdapter()", //
				"port.SamplePrimaryPort.primaryAdapterPackage()", //
				"port.SamplePrimaryPort.secondaryAdapter()", //
				"port.SamplePrimaryPort.secondaryAdapterPackage()", //
				"port.SampleSecondaryPort.application()", //
				"port.SampleSecondaryPort.adapter()", //
				"port.SampleSecondaryPort.primaryAdapter()", //
				"port.SampleSecondaryPort.primaryAdapterPackage()", //
				"port.SampleSecondaryPort.primaryPort()", //
				"port.SampleSecondaryPort.primaryPortPackage()", //
				"port.SampleSecondaryPort.secondaryAdapter()", //
				"port.SampleSecondaryPort.secondaryAdapterPackage()",
				"port.secondary.SampleSecondaryPortPackage.application()")
				.stream() //
				.map("org.jmolecules.archunit.hexagonal.pkg."::concat) //
				.collect(Collectors.toList());

		@ArchTest // GH-306
		void verifiesStrictHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal().evaluate(classes), expected);
		}

		@ArchTest // GH-306
		void verifiesSemiStrictHexagonalArchitecture(JavaClasses classes) {

			List<String> semiStrict = expected.stream()
					.filter(it -> !it.endsWith("SecondaryAdapter.application"))
					.filter(it -> !it.endsWith("SecondaryAdapterPackage.application"))
					.filter(it -> !it.endsWith("SecondaryPort.application()"))
					.filter(it -> !it.endsWith("SecondaryPortPackage.application()"))
					.filter(it -> !it.endsWith("PrimaryPort.application()"))
					.collect(Collectors.toList());

			assertExpectedViolations(ensureHexagonal(VerificationDepth.SEMI_STRICT).evaluate(classes), semiStrict);
		}

		@ArchTest // GH-306
		void verifiesLenientHexagonalArchitecture(JavaClasses classes) {

			List<String> lenient = expected.stream()
					.filter(it -> !it.endsWith(".application") && !it.endsWith(".application()"))
					.filter(it -> !it.endsWith("secondaryPort") && !it.endsWith("secondaryPortPackage"))
					.collect(Collectors.toList());

			assertExpectedViolations(ensureHexagonal(VerificationDepth.LENIENT).evaluate(classes), lenient);
		}

		@ArchTest // GH-346
		void verifiesHexagonalArchitectureWithNestedPackageAssignment(JavaClasses classes) {

			ArchRule rule = ensureHexagonal(VerificationDepth.LENIENT,
					StereotypeLookup.defaultLookup().withParentPackageTraversal());

			List<String> lenient = Stream
					.concat(expected.stream(), Stream.of("application.nested.SampleInNestedPackage.primaryAdapter"))
					.filter(it -> !it.endsWith("application") && !it.endsWith("application()"))
					.filter(it -> !it.endsWith("secondaryPort") && !it.endsWith("secondaryPortPackage"))
					.collect(Collectors.toList());

			assertExpectedViolations(rule.evaluate(classes), lenient);
		}

		@ArchTest // GH-418
		void usesStereotypeLookupFromOptions(JavaClasses classes) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults();

			// Package-level assignments are not found when looking for marker types that do not exist
			int withDefault = ensureHexagonal(options).evaluate(classes).getFailureReport().getDetails().size();

			HexagonalVerificationOptions markerOptions = options
					.withStereotypeLookup(StereotypeLookup.onMarkerType("DoesNotExist"));
			int withMarkerLookup = ensureHexagonal(markerOptions).evaluate(classes).getFailureReport().getDetails().size();

			assertThat(withMarkerLookup).isLessThan(withDefault);
		}
	}

	@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.simple")
	static class TypeBased {

		List<String> expected = Arrays.asList( //
				"SampleAdapter.application", //
				"SampleApplication.adapter", //
				"SampleApplication.primaryAdapter", //
				"SampleApplication.secondaryAdapter", //
				"SamplePrimaryAdapter.application", //
				"SamplePrimaryAdapter.secondaryPort", //
				"SampleSecondaryAdapter.application", //
				"SampleSecondaryAdapter.primaryPort", //
				"SampleSecondaryPort.application") //
				.stream() //
				.map("Field <org.jmolecules.archunit.hexagonal.simple."::concat) //
				.collect(Collectors.toList());

		@ArchTest // GH-306
		void verifiesStrictHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal().evaluate(classes), expected);
		}

		@ArchTest // GH-306
		void verifiesSemiStrictHexagonalArchitecture(JavaClasses classes) {

			// Allowed in semi-strict mode
			List<String> semiStrict = expected.stream()
					.filter(it -> !it.endsWith("SecondaryAdapter.application"))
					.filter(it -> !it.endsWith("SecondaryPort.application"))
					.collect(Collectors.toList());

			assertExpectedViolations(ensureHexagonal(VerificationDepth.SEMI_STRICT).evaluate(classes), semiStrict);
		}

		@ArchTest // GH-306
		void verifiesLenientHexagonalArchitecture(JavaClasses classes) {

			// Allowed in lenient mode
			List<String> lenient = expected.stream()
					.filter(it -> !it.endsWith(".application"))
					.filter(it -> !it.endsWith("SamplePrimaryAdapter.secondaryPort"))
					.collect(Collectors.toList());

			assertExpectedViolations(ensureHexagonal(VerificationDepth.LENIENT).evaluate(classes), lenient);
		}
	}

	@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.units")
	static class ArchitecturalUnitBased {

		List<String> expected = Stream.of( //
				"a.ApplicationA.secondaryPortB", //
				"a.HelperA.helperB", //
				"a.SecondaryAdapterA.helperB", //
				"a.SecondaryAdapterA.primaryAdapterA", //
				"a.SecondaryAdapterA.primaryAdapterNone", //
				"a.SecondaryAdapterA.primaryPortA", //
				"a.SecondaryAdapterA.secondaryPortB", //
				"b.PrimaryAdapterB.primaryPortA", //
				"d.PlainD.helperB", //
				"none.SecondaryAdapterNone.primaryAdapterNone") //
				.map(PREFIX::concat) //
				.collect(Collectors.toList());

		List<String> strict = Stream.concat(expected.stream(), Stream.of( //
				"a.ApplicationA.applicationB", //
				"a.ApplicationA.primaryPortB", //
				"a.HelperA.plainE", //
				"a.SecondaryAdapterA.primaryPortB", //
				"d.PlainD.primaryPortB", //
				"none.SecondaryAdapterNone.primaryPortB") //
				.map(PREFIX::concat)) //
				.collect(Collectors.toList());

		@ArchTest // GH-418
		void allowsApplicationAccessBetweenUnitsIfConfigured(JavaClasses classes) {

			Stream.of(VerificationDepth.values()).forEach(depth -> {

				HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
						.withDepth(depth)
						.withExposure(Exposure.PRIMARY_ADAPTERS, Exposure.PRIMARY_PORTS, Exposure.APPLICATION)
						.withReferrers(Referrer.SECONDARY_ADAPTERS, Referrer.APPLICATION);

				EvaluationResult result = ensureHexagonal(options).evaluate(classes);

				assertExpectedViolations(result, expected);
				assertThat(result.getFailureReport().getDetails())
						.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterA.primaryAdapterA>")
								.doesNotContain("architectural unit"))
						.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterA.primaryPortA>")
								.doesNotContain("architectural unit"))
						.noneSatisfy(it -> assertThat(it).contains("SecondaryAdapterA.primaryPortB>"))
						.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterA.secondaryPortB")
								.contains("SecondaryPortB belongs to architectural unit Unit B")
								.contains(
										"only exposes primary adapters (to secondary adapters), application code and primary ports."));
			});
		}

		@ArchTest // GH-418
		void onlyAllowsSecondaryAdaptersToReferToOtherUnitsByDefault(JavaClasses classes) {

			HexagonalVerificationOptions defaults = HexagonalVerificationOptions.defaults();

			assertThat(defaults.getDepth()).isEqualTo(VerificationDepth.STRICT);
			assertThat(defaults.getExposure()).containsExactly(Exposure.PRIMARY_ADAPTERS.name());
			assertThat(defaults.getReferrers()).containsExactly(Referrer.SECONDARY_ADAPTERS.name());

			assertExpectedViolations(ensureHexagonal().evaluate(classes), strict);

			Stream.of(VerificationDepth.values()).forEach(depth -> {

				HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults().withDepth(depth);

				EvaluationResult result = ensureHexagonal(options).evaluate(classes);

				assertExpectedViolations(result, strict);
				assertThat(result.getFailureReport().getDetails())
						.anySatisfy(it -> assertThat(it).contains("HelperA.plainE")
								.contains("Only secondary adapters may refer to other architectural units"));
			});
		}

		@ArchTest // GH-418
		void doesNotConsiderUnitsWhenOptingOut(JavaClasses classes) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
					.withUnitResolver(ArchitecturalUnitResolver.none());

			// Secondary adapters must not refer to primary adapters at all, as there are no units
			assertThat(ensureHexagonal(options).evaluate(classes).getFailureReport().getDetails())
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterA.primaryAdapterB"))
					.noneSatisfy(it -> assertThat(it).contains("belongs to architectural unit"))
					.noneSatisfy(it -> assertThat(it).contains("of architectural unit"));
		}

		@ArchTest // GH-418
		void onlyVerifiesTypesInScope(JavaClasses classes) {

			// All references to types of other packages are ignored
			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
					.withScope(JavaClass.Predicates.resideInAPackage("org.jmolecules.archunit.hexagonal.units.a"));

			assertExpectedViolations(ensureHexagonal(options).evaluate(classes),
					List.of("a.SecondaryAdapterA.primaryAdapterA", "a.SecondaryAdapterA.primaryPortA").stream()
							.map(PREFIX::concat).collect(Collectors.toList()));
		}

		@ArchTest // GH-418
		void appliesGlobalRulesToTypesOutsideOfUnitsOnly(JavaClasses classes) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
					.withExposure(Exposure.values())
					.withReferrers(Referrer.values());

			List<String> details = ensureHexagonal(options).evaluate(classes).getFailureReport().getDetails();

			// Secondary adapters must not refer to primary adapters unless they cross unit boundaries
			assertThat(details)
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterNone.primaryAdapterNone>")
							.doesNotContain("architectural unit"))
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterA.primaryAdapterNone>")
							.doesNotContain("architectural unit"))
					.noneSatisfy(it -> assertThat(it).contains("SecondaryAdapterNone.primaryAdapterB>"))
					.noneSatisfy(it -> assertThat(it).contains("SecondaryAdapterA.primaryAdapterB>"));
		}

		@ArchTest // GH-418
		void respectsExposureOfUnitsForTypesOutsideOfUnits(JavaClasses classes) {

			List<String> details = ensureHexagonal().evaluate(classes).getFailureReport().getDetails();

			// Primary adapters are exposed by default, primary ports are not
			assertThat(details)
					.noneSatisfy(it -> assertThat(it).contains("SecondaryAdapterNone.primaryAdapterB>"))
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterNone.primaryPortB>")
							.contains("which only exposes primary adapters (to secondary adapters)."));

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
					.withExposure(Exposure.PRIMARY_PORTS);

			assertThat(ensureHexagonal(options).evaluate(classes).getFailureReport().getDetails())
					.noneSatisfy(it -> assertThat(it).contains("SecondaryAdapterNone.primaryPortB>"))
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterNone.primaryAdapterB>")
							.contains("which only exposes primary ports."));
		}

		@ArchTest // GH-418
		void exposureCustomizationsReportReferencesToTypesOfOtherRoles(JavaClasses classes) {

			// Fields referring to a type of unit B, by the exposure that is needed for them to be accepted
			Map<Exposure, List<String>> fields = new EnumMap<>(Exposure.class);
			fields.put(Exposure.APPLICATION, List.of("ApplicationA.applicationB"));
			fields.put(Exposure.PRIMARY_PORTS,
					List.of("ApplicationA.primaryPortB", "SecondaryAdapterA.primaryPortB", "PlainD.primaryPortB"));
			fields.put(Exposure.PRIMARY_ADAPTERS, List.of("SecondaryAdapterA.primaryAdapterB", "PlainD.primaryAdapterB"));
			fields.put(Exposure.SECONDARY_PORTS, List.of("ApplicationA.secondaryPortB", "SecondaryAdapterA.secondaryPortB"));

			Map<Exposure, String> descriptions = new EnumMap<>(Exposure.class);
			descriptions.put(Exposure.APPLICATION, "which only exposes application code.");
			descriptions.put(Exposure.PRIMARY_PORTS, "which only exposes primary ports.");
			descriptions.put(Exposure.PRIMARY_ADAPTERS, "which only exposes primary adapters (to secondary adapters).");
			descriptions.put(Exposure.SECONDARY_PORTS, "which only exposes secondary ports.");

			for (Exposure exposed : Exposure.values()) {

				HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
						.withExposure(exposed)
						.withReferrers(Referrer.values());

				List<String> details = ensureHexagonal(options).evaluate(classes).getFailureReport().getDetails();

				fields.forEach((role, names) -> names.forEach(name -> {

					if (role == exposed) {
						assertNotReported(details, name);
					} else {
						assertReported(details, name, descriptions.get(exposed));
					}
				}));
			}
		}

		@ArchTest // GH-418
		void referrerCustomizationsReportReferencesFromTypesOfOtherRoles(JavaClasses classes) {

			// Fields referring to a type of another unit, by the referrer that is needed for them to be accepted
			Map<Referrer, List<String>> fields = new EnumMap<>(Referrer.class);
			fields.put(Referrer.APPLICATION,
					List.of("ApplicationA.applicationB", "ApplicationA.primaryPortB", "HelperA.plainE"));
			fields.put(Referrer.SECONDARY_ADAPTERS,
					List.of("SecondaryAdapterA.primaryAdapterB", "SecondaryAdapterA.primaryPortB"));
			fields.put(Referrer.PRIMARY_ADAPTERS, List.of("PrimaryAdapterB.primaryPortA"));

			Map<Referrer, String> descriptions = new EnumMap<>(Referrer.class);
			descriptions.put(Referrer.APPLICATION,
					"Only application code (including types without hexagonal stereotypes) may refer to other architectural units");
			descriptions.put(Referrer.SECONDARY_ADAPTERS, "Only secondary adapters may refer to other architectural units");
			descriptions.put(Referrer.PRIMARY_ADAPTERS, "Only primary adapters may refer to other architectural units");
			descriptions.put(Referrer.PRIMARY_PORTS, "Only primary ports may refer to other architectural units");
			descriptions.put(Referrer.SECONDARY_PORTS, "Only secondary ports may refer to other architectural units");

			for (Referrer allowed : Referrer.values()) {

				HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
						.withExposure(Exposure.values())
						.withReferrers(allowed);

				List<String> details = ensureHexagonal(options).evaluate(classes).getFailureReport().getDetails();

				fields.forEach((role, names) -> names.forEach(name -> {

					if (role == allowed) {
						assertNotReported(details, name);
					} else {
						assertReported(details, name, descriptions.get(allowed));
					}
				}));

				// Origins in units not using hexagonal stereotypes are not restricted in what they refer to
				assertNotReported(details, "PlainD.primaryAdapterB");
				assertNotReported(details, "PlainD.primaryPortB");
			}
		}

		@ArchTest // GH-418
		void directApplicationAccessOnlyOpensApplicationCodeAndPrimaryPortsToApplicationCode(JavaClasses classes) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults().withDirectApplicationAccess();

			// Exposure and referrers stay untouched
			assertThat(options.getExposure()).containsExactly(Exposure.PRIMARY_ADAPTERS.name());
			assertThat(options.getReferrers()).containsExactly(Referrer.SECONDARY_ADAPTERS.name());

			List<String> details = ensureHexagonal(options).evaluate(classes).getFailureReport().getDetails();

			assertNotReported(details, "a.ApplicationA.applicationB");
			assertNotReported(details, "a.ApplicationA.primaryPortB");
			assertNotReported(details, "d.PlainD.primaryPortB");
			assertNotReported(details, "a.SecondaryAdapterA.primaryAdapterB");
			assertReported(details, "a.SecondaryAdapterA.primaryPortB", "which only exposes primary adapters");
			assertReported(details, "a.ApplicationA.secondaryPortB",
					"Only secondary adapters, as well as application code and ports (to application code and primary ports), may refer");

			// Explicitly exposed roles are available to all referrers
			details = ensureHexagonal(options.withExposure(Exposure.PRIMARY_ADAPTERS, Exposure.PRIMARY_PORTS))
					.evaluate(classes).getFailureReport().getDetails();

			assertNotReported(details, "a.SecondaryAdapterA.primaryPortB");
			assertNotReported(details, "a.ApplicationA.applicationB");
		}

		private static void assertReported(List<String> details, String field, String reason) {

			assertThat(details).as(field)
					.anySatisfy(it -> assertThat(it).contains(field + ">").contains(reason));
		}

		private static void assertNotReported(List<String> details, String field) {
			assertThat(details).as(field).noneSatisfy(it -> assertThat(it).contains(field + ">"));
		}
	}

	@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.exposure")
	static class ExposureBased {

		static final String PREFIX = "Field <org.jmolecules.archunit.hexagonal.exposure.";

		@ArchTest // GH-418
		void onlyAllowsReferencesToTypesExposedByUnit(JavaClasses classes) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
					.withExposure(Exposure.values())
					.withReferrers(Referrer.values());

			EvaluationResult result = ensureHexagonal(options).evaluate(classes);

			assertExpectedViolations(result, Stream.of( //
					"client.SecondaryAdapterC.primaryAdapterP", //
					"client.ApplicationC.applicationP", //
					"plain.PlainC.applicationP", //
					"restricted.ApplicationR.primaryPortP", //
					"plain.PlainC.helperD", //
					"declared.HelperD.applicationO") //
					.map(PREFIX::concat) //
					.collect(Collectors.toList()));

			assertThat(result.getFailureReport().getDetails())
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterC.primaryAdapterP")
							.contains("PrimaryAdapterP belongs to architectural unit Ports only")
							.contains("which only exposes primary ports."))
					.anySatisfy(it -> assertThat(it).contains("PlainC.helperD")
							.contains("HelperD belongs to architectural unit Declared"));
		}

		@ArchTest // GH-418
		void declaredExposureAndReferrersOverrideDefaultSettings(JavaClasses classes) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults();

			// Application code still must not refer to other units unless a unit allows it explicitly (liberal), but the
			// types exposed explicitly are accessible to others
			assertExpectedViolations(ensureHexagonal(options).evaluate(classes), Stream.of( //
					"client.SecondaryAdapterC.primaryAdapterP", //
					"client.ApplicationC.primaryPortP", //
					"client.ApplicationC.applicationP", //
					"client.ApplicationC.applicationO", //
					"plain.PlainC.applicationP", //
					"restricted.ApplicationR.primaryPortP", //
					"plain.PlainC.helperD", //
					"declared.HelperD.applicationO") //
					.map(PREFIX::concat) //
					.collect(Collectors.toList()));
		}
	}

	@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.implicit")
	static class CustomUnitResolverBased {

		private static final String BASE = "org.jmolecules.archunit.hexagonal.implicit";

		// Treats each direct sub-package of the base package as a unit, like Spring Modulith does by default
		ArchitecturalUnitResolver resolver = type -> {

			String name = type.getPackageName();

			if (!name.startsWith(BASE + ".")) {
				return null;
			}

			String unit = name.substring(BASE.length() + 1).split("\\.")[0];

			return ArchitecturalUnit.of(unit, unit, null, type.getPackage(), null);
		};

		@ArchTest // GH-418
		void doesNotDetectUnitsWithoutAnnotationsByDefault(JavaClasses classes) {

			// Without annotations there are no units, so the secondary adapter referring to a primary adapter is a plain
			// layer violation
			assertExpectedViolations(ensureHexagonal().evaluate(classes), List.of("SecondaryAdapterX.primaryAdapterY"));
		}

		@ArchTest // GH-418
		void usesCustomUnitResolver(JavaClasses classes) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults().withUnitResolver(resolver);

			assertExpectedViolations(ensureHexagonal(options).evaluate(classes),
					List.of("PrimaryAdapterX.primaryAdapterY"));
		}
	}

	@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.nested")
	static class NestedUnitBased {

		@ArchTest // GH-418
		void verifiesNestedUnitsIndividually(JavaClasses classes) {

			// The layer rules only apply within a unit, i.e. not to references between the outer and the nested one, which
			// are governed by the units' exposure instead. Types in sub-packages belong to the nearest unit.
			assertExpectedViolations(ensureHexagonal().evaluate(classes), Stream.of( //
					"outer.SecondaryAdapterO.primaryAdapterO", //
					"outer.inner.SecondaryAdapterI.primaryAdapterI", //
					"outer.inner.deeper.SecondaryAdapterDeep.primaryAdapterI") //
					.map("Field <org.jmolecules.archunit.hexagonal.nested."::concat) //
					.collect(Collectors.toList()));
		}

		@ArchTest // GH-418
		void respectsExposureBetweenOuterAndNestedUnits(JavaClasses classes) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
					.withExposure(Exposure.PRIMARY_PORTS);

			// Primary adapters are not exposed anymore, so the references between the units are reported now
			List<String> details = ensureHexagonal(options).evaluate(classes).getFailureReport().getDetails();

			assertThat(details)
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterO.primaryAdapterI>")
							.contains("PrimaryAdapterI belongs to architectural unit Inner"))
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterI.primaryAdapterO>")
							.contains("PrimaryAdapterO belongs to architectural unit Outer"))
					.anySatisfy(it -> assertThat(it).contains("SecondaryAdapterDeep.primaryAdapterO>")
							.contains("PrimaryAdapterO belongs to architectural unit Outer"));
		}
	}

	private static void assertExpectedViolations(EvaluationResult result, List<String> expected) {

		List<String> filtered = expected;

		List<String> messages = result.getFailureReport().getDetails();
		assertThat(messages.stream().distinct()).hasSameSizeAs(filtered);

		Stream<String> consumed = messages.stream().distinct()
				.filter(message -> !filtered.stream().anyMatch(message::contains));
		assertThat(consumed).isEmpty();
	}
}
