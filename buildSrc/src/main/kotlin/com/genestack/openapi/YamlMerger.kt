package com.genestack.openapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

class YamlMerger {

    fun mergeYamlFiles(outputFilePath: String, vararg inputFilePaths: String) {
        val objectMapper = ObjectMapper(YAMLFactory())


        // acc.deepMerge(node)
        val mergedNode = inputFilePaths
            .map { readYamlFile(objectMapper, it) }
            .reduce { acc, node -> deepMerge(objectMapper, acc, node) }

        writeYamlFile(objectMapper, outputFilePath, mergedNode)
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

    private fun readYamlFile(objectMapper: ObjectMapper, filePath: String): JsonNode {
        val file = File(filePath)
        return if (file.exists()) {
            objectMapper.readTree(file)
        } else {
            throw IllegalArgumentException("File not found: $filePath")
        }
    }

    private fun writeYamlFile(objectMapper: ObjectMapper, filePath: String, node: JsonNode) {
        val file = File(filePath)
        objectMapper.writeValue(file, node)
    }
}
