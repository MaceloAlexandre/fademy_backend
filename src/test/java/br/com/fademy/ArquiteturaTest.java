package br.com.fademy;

import br.com.fademy.infrastructure.database.DatabaseConfig;
import br.com.fademy.infrastructure.database.DatabaseManager;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "br.com.fademy", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String[] FRAMEWORKS = {
            "org.springframework..", "jakarta.persistence..", "org.bson..", "com.mongodb.."
    };

    @ArchTest
    static final ArchRule dominioNaoDependeDeOutrasCamadas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dominioNaoDependeDeFramework = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule aplicacaoNaoDependeDaInfraestrutura = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule aplicacaoNaoDependeDeFramework = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule soDatabaseConfigChamaGetInstance = noClasses()
            .that().doNotHaveFullyQualifiedName(DatabaseConfig.class.getName())
            .should().callMethod(DatabaseManager.class, "getInstance");
}
