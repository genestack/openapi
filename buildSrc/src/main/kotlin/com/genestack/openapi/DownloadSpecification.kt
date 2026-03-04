package com.genestack.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import org.gradle.kotlin.dsl.property

import org.apache.commons.io.IOUtils
import java.io.FileOutputStream
import java.net.URI
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.util.Base64

abstract class DownloadSpecification : DefaultTask() {

    @Input
    val version = project.objects.property<String>()

    @Input
    @Optional
    val registryUsername = project.objects.property<String>()

    @Input
    @Optional
    val registryPassword = project.objects.property<String>()

    @Input
    val releaseRegistryUrl = project.objects.property<String>()

    @Input
    val snapshotRegistryUrl = project.objects.property<String>()

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val versionValue = version.get()
        val pathInRegistry = "odm-oas/processor-controller"
        val archiveName = "processor-controller-odm-oas-${versionValue}.tar.gz"

        // Regex pattern for strict semantic versioning (x.y.z)
        val semverPattern = Regex("^\\d+\\.\\d+\\.\\d+$")
        val isRelease = semverPattern.matches(versionValue)

        val registryUrl = if (isRelease) {
            releaseRegistryUrl.get()
        } else {
            snapshotRegistryUrl.get()
        }

        val downloadUrl = "${registryUrl}/${pathInRegistry}/${archiveName}"
        val url = URI(downloadUrl).toURL()

        // Create HTTP connection and set basic authentication
        val connection = url.openConnection() as HttpURLConnection

        try {

            // Create basic auth header
            val credentials = "${registryUsername.get()}:${registryPassword.get()}"
            val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
            connection.setRequestProperty("Authorization", "Basic $encodedCredentials")

            // Use as a stream
            connection.inputStream.use { inputStream ->
                GZIPInputStream(inputStream).use { gzipStream ->
                    TarArchiveInputStream(gzipStream).use { tarStream ->
                        var entry = tarStream.nextEntry

                        while (entry != null) {
                            if (!entry.isDirectory) {
                                // Extract the first file found
                                val outputFileHandle = outputFile.get().asFile
                                outputFileHandle.parentFile.mkdirs()

                                FileOutputStream(outputFileHandle).use { outputStream ->
                                    IOUtils.copy(tarStream, outputStream)
                                }

                                logger.info("Extracted file: ${entry.name} to ${outputFileHandle.absolutePath}")
                                break
                            }
                            entry = tarStream.nextEntry
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
