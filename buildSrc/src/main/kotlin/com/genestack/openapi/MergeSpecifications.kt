package com.genestack.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.yaml.YAMLFactory


abstract class MergeSpecifications : DefaultTask() {

    @get:InputFiles
    abstract val inputFiles: ListProperty<RegularFile>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    @get:Optional
    abstract val outputFileJson: RegularFileProperty

    @TaskAction
    fun merge() {
        val yamlMapper = ObjectMapper(YAMLFactory())
        val jsonMapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()

        val mergedNode = inputFiles
            .get().map { it.asFile }
            .filterNot { it == outputFile.get().asFile }
            .map { yamlMapper.readTree(it) }
            .reduce { acc, node -> yamlMapper.updateValue(acc, node) }

        yamlMapper.writeValue(outputFile.get().asFile, mergedNode)

        if (outputFileJson.isPresent) {
            jsonMapper.writeValue(outputFileJson.get().asFile, mergedNode)
        }
    }
}
