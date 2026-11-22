package com.payflow.orders;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Enforces package-by-feature with Clean Architecture layers inside each feature.
 * Layers point inwards: infrastructure -> application -> domain.
 * Features (paymentorder, ...) may use shared, but shared never uses a feature.
 * The build fails if a class breaks any rule.
 */
@AnalyzeClasses(packages = "com.payflow.orders", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnDomain =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructure =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule sharedDoesNotDependOnFeatures =
            noClasses().that().resideInAPackage("com.payflow.orders.shared..")
                    .should().dependOnClassesThat().resideInAPackage("com.payflow.orders.paymentorder..");

    @ArchTest
    static final ArchRule featuresHaveNoCycles =
            slices().matching("com.payflow.orders.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule webDoesNotUseOutboxInternals =
            noClasses().that().resideInAPackage("..infrastructure.web..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.outbox..");
}
