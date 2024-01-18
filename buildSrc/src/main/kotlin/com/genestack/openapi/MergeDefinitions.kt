package com.genestack.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles


abstract class MergeDefinitions : DefaultTask() {

    @get:InputFiles
    abstract val inputFiles: ListProperty<RegularFile>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun merge() {
        val yamlMerger = YamlMerger()
        val destinationFile = outputFile.get().asFile
        destinationFile.parentFile.mkdirs()
        destinationFile.delete()
        destinationFile.writeText("Hello!")

//        yamlMerger.mergeYamlFiles(outputFile, *inputFiles)
//        println("YAML files merged successfully.")

    }
}
