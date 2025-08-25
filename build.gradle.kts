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

val processorsControllerVersion: String = System.getenv("PROCESSORS_CONTROLLER_VERSION")
val processorsControllerFileName = "processorsController.yaml"
val processorsControllerFilePath = "${sourceDirectory}/${processorsControllerFileName}"

val odmFileName = "odm.yaml"
val odmFilePath = "${sourceDirectory}/${odmFileName}"

val openapiVersion: String = System.getenv("OPENAPI_VERSION")
val openapiFileName = "openapi.yaml"
val openapiFilePath = "${sourceDirectory}/${openapiFileName}"

tasks {
    val downloadSpec by registering(DownloadSpecification::class) {
        version.set(processorsControllerVersion)
        registryUsername.set(System.getenv("NEXUS_USER"))
        registryPassword.set(System.getenv("NEXUS_PASSWORD"))
        releaseRegistryUrl.set(System.getenv("RAW_REGISTRY_RELEASES"))
        snapshotRegistryUrl.set(System.getenv("RAW_REGISTRY_SNAPSHOTS"))
        outputFile.set(layout.projectDirectory.file(processorsControllerFilePath))
    }
    val mergeSpecifications by registering(MergeSpecifications::class) {
        dependsOn(downloadSpec)
        inputFiles = listOf(odmFilePath, processorsControllerFilePath)
            .sorted().map { layout.projectDirectory.file(it) }
        outputFile = layout.projectDirectory.file(openapiFilePath)
    }
    val generateOdmApiPython by registering(GenerateTask::class) {
        dependsOn(mergeSpecifications)
        generatorName.set("python")
        inputSpec.set(openapiFilePath)
        outputDir.set("$rootDir/generated/python")
        packageName.set("odm_api")
        gitUserId.set("genestack")
        gitRepoId.set("openapi")
        nameMappings.set(mapOf("genestack:accession" to "genestackaccession"))
        skipValidateSpec.set(true)
        configOptions = mapOf(
            "packageVersion" to openapiVersion,
            // Workaround for https://github.com/OpenAPITools/openapi-generator/issues/21619
            // The second version asks for license, which we can't provide due to unavailability of
            // "licenseName" and "licenseUrl" fields in the specification for python generator.
            "poetry1" to "true"
//            "disallowAdditionalPropertiesIfNotPresent" to "true"
        )
    }
    val generateOdmApiR by registering(GenerateTask::class) {
        dependsOn(mergeSpecifications)
        generatorName.set("r")
        inputSpec.set(openapiFilePath)
        outputDir.set("$rootDir/generated/r")
        packageName.set("odmApi")
        gitUserId.set("genestack")
        gitRepoId.set("openapi")
        nameMappings.set(mapOf("genestack:accession" to "genestackaccession"))
        skipValidateSpec.set(true)
        configOptions = mapOf(
            "packageVersion" to openapiVersion
//            "disallowAdditionalPropertiesIfNotPresent" to "true"
        )
    }
    val generateOdmApiPostmanCollection by registering(GenerateTask::class) {
        dependsOn(mergeSpecifications)
        generatorName.set("postman-collection")
        inputSpec.set(openapiFilePath)
        outputDir.set("$rootDir/generated/postman-collection")
        packageName.set("odm-api")
        gitUserId.set("genestack")
        gitRepoId.set("openapi")
        nameMappings.set(mapOf("genestack:accession" to "genestackaccession"))
        skipValidateSpec.set(true)
        configOptions = mapOf(
            "packageVersion" to openapiVersion
//            "disallowAdditionalPropertiesIfNotPresent" to "true"
        )
    }
    val generateAll by registering(GradleBuild::class) {
        file("$rootDir/generated").deleteRecursively()
        tasks = listOf(
            generateOdmApiPython.name,
            generateOdmApiR.name,
            generateOdmApiPostmanCollection.name
        )
    }
}
