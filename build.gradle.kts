/*
 * Copyright (c) 2011-2025 Genestack Limited
 * All Rights Reserved
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF GENESTACK LIMITED
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

import com.genestack.openapi.DownloadSpecification
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import com.genestack.openapi.MergeSpecifications
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.Path as KotlinPath

plugins {
    alias(libs.plugins.openapi.generator) apply true
}

val sourceDirectory = "$rootDir/openapi/v1"

val processorControllerVersion = System.getenv("PROCESSOR_CONTROLLER_VERSION") ?: "1.0.0"
val processorControllerFileName = "processorController.yaml"
val processorControllerFilePath = "${sourceDirectory}/${processorControllerFileName}"

val openApiVersion = System.getenv("OPENAPI_VERSION") ?: "1.0.0"
val mergedFileName = "odmApi.yaml"
val mergedFilePath = "${sourceDirectory}/${mergedFileName}"

val sourceFileList = KotlinPath(sourceDirectory)
    .listDirectoryEntries("*.yaml")
    .sorted()
    .map { layout.projectDirectory.file("${sourceDirectory}/${it.name}") }

tasks {
    val generateOdmApiPython by registering(GenerateTask::class) {
        generatorName.set("python")
        inputSpec.set(mergedFilePath)
        outputDir.set("$rootDir/generated/python")
        packageName.set("odm_api")
        gitUserId.set("genestack")
        gitRepoId.set("openapi")
        nameMappings.set(mapOf("genestack:accession" to "genestackaccession"))
        configOptions = mapOf(
            "packageVersion" to openApiVersion
//            "disallowAdditionalPropertiesIfNotPresent" to "true"
        )
    }
    val generateOdmApiR by registering(GenerateTask::class) {
        generatorName.set("r")
        inputSpec.set(mergedFilePath)
        outputDir.set("$rootDir/generated/r")
        packageName.set("odmApi")
        gitUserId.set("genestack")
        gitRepoId.set("openapi")
        nameMappings.set(mapOf("genestack:accession" to "genestackaccession"))
        configOptions = mapOf(
            "packageVersion" to openApiVersion
//            "disallowAdditionalPropertiesIfNotPresent" to "true"
        )
    }
    val generateOdmApiPostmanCollection by registering(GenerateTask::class) {
        generatorName.set("postman-collection")
        inputSpec.set(mergedFilePath)
        outputDir.set("$rootDir/generated/postman-collection")
        packageName.set("odm-api")
        gitUserId.set("genestack")
        gitRepoId.set("openapi")
        nameMappings.set(mapOf("genestack:accession" to "genestackaccession"))
        configOptions = mapOf(
            "packageVersion" to openApiVersion
//            "disallowAdditionalPropertiesIfNotPresent" to "true"
        )
    }
    val downloadSpec by registering(DownloadSpecification::class) {
        version.set(processorControllerVersion)
        registryUsername.set(System.getenv("NEXUS_USER"))
        registryPassword.set(System.getenv("NEXUS_PASSWORD"))
        releaseRegistryUrl.set(System.getenv("RAW_REGISTRY_RELEASES"))
        snapshotRegistryUrl.set(System.getenv("RAW_REGISTRY_SNAPSHOTS"))
        outputFile.set(layout.projectDirectory.file(processorControllerFilePath))
    }
    val mergeSpecifications by registering(MergeSpecifications::class) {
        dependsOn(downloadSpec)
        inputFiles = sourceFileList
        outputFile = layout.projectDirectory.file(mergedFilePath)
    }

    val generateAll by registering(GradleBuild::class) {
        dependsOn(mergeSpecifications)
        file("$rootDir/generated").deleteRecursively()
        tasks = listOf(
            generateOdmApiPython.name,
            generateOdmApiR.name,
            generateOdmApiPostmanCollection.name
        )
    }
}
