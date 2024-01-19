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
val mergedFileName = "merged.yaml"
val fileNameList = KotlinPath(sourceDirectory)
    .listDirectoryEntries("*.yaml")
    .filterNot { it.name == mergedFileName }
val tasksList = fileNameList
        .map { it.name.replace(".yaml", "") }

fun String.camelToKebabCase(): String {
    val pattern = "(?<=.)[A-Z]".toRegex()
    return this.replace(pattern, "-$0").lowercase()
}

fun String.camelToSnakeCase(): String {
    val pattern = "(?<=.)[A-Z]".toRegex()
    return this.replace(pattern, "_$0").lowercase()
}

fun String.kebabToCamelCase(): String {
    val pattern = "(-)([a-z])".toRegex()
    return this.replace(pattern) { it.groupValues[2].uppercase() }
}

val sourceFileList = fileNameList.map {
    layout.projectDirectory.file("${sourceDirectory}/${it.name}")
}

tasks {
    for (task in tasksList) {
        register(task + "Python", GenerateTask::class) {
            generatorName.set("python")
            inputSpec.set("${sourceDirectory}/${task}.yaml")
            outputDir.set("$rootDir/generated/python/${task.camelToKebabCase()}")
            packageName.set(task.camelToSnakeCase())
            gitUserId.set("genestack")
            gitRepoId.set("odm-openapi")
            configOptions = mapOf(
                    "packageVersion" to openApiVersion
            )
            skipValidateSpec = true
        }
        register(task + "R", GenerateTask::class) {
            generatorName.set("r")
            inputSpec.set("${sourceDirectory}/${task}.yaml")
            outputDir.set("$rootDir/generated/r/${task.kebabToCamelCase()}")
            packageName.set(task.kebabToCamelCase())
            gitUserId.set("genestack")
            gitRepoId.set("odm-openapi")
            configOptions = mapOf(
                    "packageVersion" to openApiVersion
            )
            skipValidateSpec = true
        }
    }

    register("mergeDefinitions", MergeDefinitions::class) {
        inputFiles = sourceFileList
        outputFile = layout.projectDirectory.file("${sourceDirectory}/${mergedFileName}")
    }

    register("generateMergedRSdk", GenerateTask::class) {
        dependsOn("mergeDefinitions")
        generatorName.set("r")
        inputSpec.set("${sourceDirectory}/merged.yaml")
        outputDir.set("$rootDir/generated/r/odmApiSdk")
        packageName.set("odmApiSdk")
        gitUserId.set("genestack")
        gitRepoId.set("odm-openapi")
        configOptions = mapOf(
            "packageVersion" to openApiVersion
        )
        skipValidateSpec = true
    }

    register("generateMergedPythonSdk", GenerateTask::class) {
        dependsOn("mergeDefinitions")
        generatorName.set("python")
        inputSpec.set("${sourceDirectory}/merged.yaml")
        outputDir.set("$rootDir/generated/python/odm-api-sdk")
        packageName.set("odm_api_sdk")
        gitUserId.set("genestack")
        gitRepoId.set("odm-openapi")
        configOptions = mapOf(
            "packageVersion" to openApiVersion
        )
        skipValidateSpec = true
    }

    val generateAllApiSdks by registering(GradleBuild::class) {
        tasks = tasksList
            .flatMap { listOf(it + "Python", it + "R") }
            .plus("generateMergedRSdk").
            plus("generateMergedPythonSdk")
    }
}
