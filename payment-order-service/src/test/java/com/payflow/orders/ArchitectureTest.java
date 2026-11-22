package com.payflow.orders;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the dependency rule: adapters -> application -> domain.
 * The build fails if a class breaks it.
 */
@AnalyzeClasses(packages = "com.payflow.orders", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnNothingInsideTheService =
            noClasses().that().resideInAPackage("..orders.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..orders.application..", "..orders.adapter..", "..orders.config..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters =
            noClasses().that().resideInAPackage("..orders.application..")
                    .should().dependOnClassesThat().resideInAPackage("..orders.adapter..");

    @ArchTest
    static final ArchRule webAdapterDoesNotUseOutboxAdapter =
            noClasses().that().resideInAPackage("..orders.adapter.web..")
                    .should().dependOnClassesThat().resideInAPackage("..orders.adapter.outbox..");

    @ArchTest
    static final ArchRule outboxAdapterDoesNotUseWebAdapter =
            noClasses().that().resideInAPackage("..orders.adapter.outbox..")
                    .should().dependOnClassesThat().resideInAPackage("..orders.adapter.web..");
}
