/*
 * Copyright (c) 2011-2025 Genestack Limited
 * All Rights Reserved
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF GENESTACK LIMITED
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import com.genestack.openapi.MergeSpecifications
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.Path as KotlinPath

plugins {
    alias(libs.plugins.openapi.generator)
}

val sourceDirectory = "$rootDir/openapi/v1"

val openApiVersion: String = System.getenv("OPENAPI_VERSION")
val mergedFileName = "odmApi.yaml"
val mergedFilePath = "${sourceDirectory}/${mergedFileName}"

tasks {
    val mergeSpecifications by registering(MergeSpecifications::class) {
        inputFiles.set(provider { // provider to calculate during runtime, not configuration loading time
            KotlinPath(sourceDirectory)
                .listDirectoryEntries("*.yaml")
                .sorted()
                .map { layout.projectDirectory.file("${sourceDirectory}/${it.name}") }
        })
        outputFile.set(layout.projectDirectory.file(mergedFilePath))
    }
    val generateOdmApiPython by registering(GenerateTask::class) {
        dependsOn(mergeSpecifications)
        generatorName.set("python")
        inputSpec.set(mergedFilePath)
        outputDir.set("$rootDir/generated/python")
        packageName.set("odm_api")
        gitUserId.set("genestack")
        gitRepoId.set("openapi")
        nameMappings.set(mapOf("genestack:accession" to "genestackaccession"))
        configOptions = mapOf(
            "packageVersion" to openApiVersion,
            // Workaround for https://github.com/OpenAPITools/openapi-generator/issues/21619
            // The second version asks for license, which we can't provide due to unavailability of
            // "licenseName" and "licenseUrl" fields in the specification for python generator.
            "poetry1" to "true",
            // All float fields accept "NaN", "Infinity", "-Infinity" values
            "mapNumberTo" to "float"
//            "disallowAdditionalPropertiesIfNotPresent" to "true"
        )
    }
    val generateAll by registering(GradleBuild::class) {
        file("$rootDir/generated").deleteRecursively()
        tasks = listOf(
            generateOdmApiPython.name
        )
    }
}
