import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.Path as KotlinPath

plugins {
    alias(libs.plugins.openapi.generator) apply true
    `custom-greetings`
}

val openApiVersion: String = System.getenv("ODM_OPENAPI_VERSION") ?: "1.0.0"
val openApiSpecPath = "$rootDir/openapi/v1"
val fileNameList = KotlinPath(openApiSpecPath).listDirectoryEntries("*.yaml")
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


tasks {
    for (task in tasksList) {
        register(task + "Python", GenerateTask::class) {
            generatorName.set("python")
            inputSpec.set("$rootDir/openapi/v1/$task.yaml")
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
            inputSpec.set("$rootDir/openapi/v1/$task.yaml")
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

    val generateAllApiClients by registering(GradleBuild::class) {
        file("$rootDir/generated").deleteRecursively()
        tasks = tasksList.flatMap { listOf(it + "Python", it + "R") }
    }
}
