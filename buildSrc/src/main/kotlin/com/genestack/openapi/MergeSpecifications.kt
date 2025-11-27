package com.genestack.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputDirectory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory


abstract class MergeSpecifications : DefaultTask() {

    @get:InputDirectory
    abstract val inputDir: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun merge() {
        val objectMapper = ObjectMapper(YAMLFactory())
        val mergedNode = inputDir.get().asFile.listFiles { file ->
            !file.name.contains("{Role}") && file.name.endsWith(".yaml")
        }.sorted()
            .map { objectMapper.readTree(it) }
            .reduce { acc, node -> objectMapper.updateValue(acc, node) }
        objectMapper.writeValue(outputFile.get().asFile, mergedNode)
    }
}
