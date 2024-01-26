import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import com.genestack.openapi.MergeDefinitions
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.Path as KotlinPath

plugins {
    alias(libs.plugins.openapi.generator) apply true
}

val openApiVersion: String = System.getenv("ODM_OPENAPI_VERSION") ?: "1.0.0"
val sourceDirectory = "$rootDir/openapi/v1"
val fileNameList = KotlinPath(sourceDirectory).listDirectoryEntries("*.yaml")
val mergedFileName = "odmApi.yaml"
val sourceFileList = fileNameList.map {
    layout.projectDirectory.file("${sourceDirectory}/${it.name}")
}

tasks {
    register("generateOdmApiPython", GenerateTask::class) {
        generatorName.set("python")
        inputSpec.set("${sourceDirectory}/odmApi.yaml")
        outputDir.set("$rootDir/generated/python/odm-api")
        packageName.set("odm_api")
        gitUserId.set("genestack")
        gitRepoId.set("odm-openapi")
        configOptions = mapOf(
            "packageVersion" to openApiVersion
        )
    }
    register("generateOdmApiR", GenerateTask::class) {
        generatorName.set("r")
        inputSpec.set("${sourceDirectory}/odmApi.yaml")
        outputDir.set("$rootDir/generated/r/odm-api")
        packageName.set("odmApi")
        gitUserId.set("genestack")
        gitRepoId.set("odm-openapi")
        configOptions = mapOf(
            "packageVersion" to openApiVersion
        )
    }
    register("generateOdmApiPostmanCollection", GenerateTask::class) {
        generatorName.set("postman-collection")
        inputSpec.set("${sourceDirectory}/odmApi.yaml")
        outputDir.set("$rootDir/generated/postman-collection/odm-api")
        packageName.set("odm-api")
        gitUserId.set("genestack")
        gitRepoId.set("odm-openapi")
        configOptions = mapOf(
            "packageVersion" to openApiVersion
        )
    }
    // Should be used in pre-commit
    register("mergeDefinitions", MergeDefinitions::class) {
        inputFiles = sourceFileList
        outputFile = layout.projectDirectory.file("${sourceDirectory}/${mergedFileName}")
    }
    val generateAll by registering(GradleBuild::class) {
        file("$rootDir/generated").deleteRecursively()
        tasks = listOf("generateOdmApiPython", "generateOdmApiR", "generateOdmApiPostmanCollection")
    }
}
