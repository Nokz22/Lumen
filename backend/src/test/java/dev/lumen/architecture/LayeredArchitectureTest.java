package dev.lumen.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import jakarta.persistence.Entity;

@AnalyzeClasses(packages = "dev.lumen", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_SPRING = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule ENTITIES_ARE_NOT_REFERENCED_IN_PRESENTATION = noClasses()
            .that()
            .resideInAPackage("..presentation..")
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule DTOS_DO_NOT_EXIST_IN_DOMAIN = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .haveSimpleNameEndingWith("Dto")
            .orShould()
            .haveSimpleNameEndingWith("Request")
            .orShould()
            .haveSimpleNameEndingWith("Response");

    @ArchTest
    static final ArchRule CONTROLLERS_DO_NOT_DEPEND_ON_REPOSITORIES = noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule REPOSITORIES_ARE_ONLY_ACCESSED_FROM_DOMAIN_OR_APPLICATION_OR_INFRASTRUCTURE = classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .and()
            .resideInAPackage("..domain..")
            .should()
            .onlyBeAccessed()
            .byAnyPackage("..domain..", "..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule LAYERED_ARCHITECTURE = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Presentation")
            .definedBy("..presentation..")
            .layer("Application")
            .definedBy("..application..")
            .layer("Domain")
            .definedBy("..domain..")
            .layer("Infrastructure")
            .definedBy("..infrastructure..")
            .whereLayer("Presentation")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Presentation")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Presentation", "Application", "Infrastructure")
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer();
}
