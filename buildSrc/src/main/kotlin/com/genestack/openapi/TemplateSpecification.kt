package com.genestack.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import java.io.File

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory


abstract class TemplateSpecification : DefaultTask() {

    @get:InputFiles
    abstract val inputFiles: ListProperty<RegularFile>

    @get:OutputDirectory
    abstract val outputDir: RegularFileProperty

    @TaskAction
    fun template() {
        inputFiles.get().map { it.asFile }
            .forEach {
                writeTemplated(it, "User")
                writeTemplated(it, "Curator")
            }
    }

    private fun writeTemplated(spec: File, role: String) {
        val content = spec.readText()
        val outputContent = StringBuilder()

        // Role in lowercase for substitutions
        val roleLowercase = role.lowercase()

        // Define regex patterns for role matching
        val rolePattern = """\{Role=([^}]+)\}""".toRegex()
        val sectionStartPattern = """##\s*\{Role=([^}]+)\}""".toRegex()
        val sectionEndPattern = """##\s*end\s*\{Role=([^}]+)\}""".toRegex()
        val lineCommentPattern = """#\{Role=([^}]+)\}""".toRegex()

        // Flag to track if we're inside a conditional section
        var insideConditionalSection = false
        var shouldIncludeSection = false
        var sectionRole = ""

        // Process line by line
        content.lineSequence().withIndex().forEach { (index, line) ->
            // Check if this line is the start of a conditional section
            val sectionStartMatch = sectionStartPattern.find(line)
            if (sectionStartMatch != null) {
                if (insideConditionalSection) {
                    throw IllegalStateException("Nested conditional sections are not allowed. Error in file: ${spec.name}, line ${index}")
                }

                insideConditionalSection = true
                sectionRole = sectionStartMatch.groupValues[1]
                shouldIncludeSection = (sectionRole == role)
                // Skip this marker line
                return@forEach
            }

            // Check if this line is the end of a conditional section
            val sectionEndMatch = sectionEndPattern.find(line)
            if (sectionEndMatch != null) {
                if (!insideConditionalSection) {
                    throw IllegalStateException("Found end marker without start marker. Error in file: ${spec.name}, line ${index}")
                }

                val endRole = sectionEndMatch.groupValues[1]
                if (endRole != sectionRole) {
                    throw IllegalStateException("Mismatched roles in section markers: $sectionRole vs $endRole. Error in file: ${spec.name}, line ${index}")
                }

                insideConditionalSection = false
                shouldIncludeSection = false
                // Skip this marker line
                return@forEach
            }

            // If we're inside a conditional section and shouldn't include it, skip this line
            if (insideConditionalSection && !shouldIncludeSection) {
                return@forEach
            }

            // Handle single line conditional comments: #{Role=X}
            val lineCommentMatch = lineCommentPattern.find(line)
            if (lineCommentMatch != null) {
                val commentRole = lineCommentMatch.groupValues[1]
                if (commentRole == role) {
                    // Keep the line but remove the comment
                    val processedLine = line.replace(lineCommentPattern, "").trimEnd()
                    outputContent.appendLine(processedLine)
                }
                // If role doesn't match, skip this line
                return@forEach
            }

            // Process regular lines with substitutions
            val processedLine = line.replace("{Role}", role).replace("{role}", roleLowercase)
            outputContent.appendLine(processedLine)
        }

        // Check if we ended with an unclosed section
        if (insideConditionalSection) {
            throw IllegalStateException("Unclosed conditional section for role: $sectionRole. Error in file: ${spec.name}")
        }

        // Create output file with appropriate name
        val outputFileName = spec.name.replace("{Role}", role)
        val outputFile = File(outputDir.get().asFile, outputFileName)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(outputContent.toString())
    }
}
