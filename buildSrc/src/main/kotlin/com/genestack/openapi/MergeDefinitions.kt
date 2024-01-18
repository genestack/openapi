package com.genestack.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File


abstract class MergeDefinitions : DefaultTask() {

    @get:InputFiles
    abstract val inputFiles: ListProperty<RegularFile>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun merge() {
        val objectMapper = ObjectMapper(YAMLFactory())

        val outputFileNew = outputFile.get().asFile
        outputFileNew.parentFile.mkdirs()
        outputFileNew.delete()

        val inputFilesNew = inputFiles.get().map { it.asFile }

        val mergedNode = inputFilesNew
            .map { objectMapper.readTree(it) }
            .reduce { acc, node -> deepMerge(objectMapper, acc, node) }

        objectMapper.writeValue(outputFileNew, mergedNode)

    }

    private fun deepMerge(objectMapper: ObjectMapper, target: JsonNode, source: JsonNode): JsonNode {
        if (target is ObjectNode && source is ObjectNode) {
            source.fieldNames().forEach { fieldName ->
                val targetNode = target.get(fieldName)
                val sourceNode = source.get(fieldName)

                if (targetNode is ObjectNode && sourceNode is ObjectNode) {
                    target.set(fieldName, deepMerge(objectMapper, targetNode, sourceNode))
                } else {
                    target.set(fieldName, sourceNode)
                }
            }
        }
        return target
    }
}
