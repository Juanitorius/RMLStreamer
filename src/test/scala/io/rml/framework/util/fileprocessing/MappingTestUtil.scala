package io.rml.framework.util.fileprocessing

import java.io.File
import java.nio.file.Paths

import io.rml.framework.core.extractors.MappingReader
import io.rml.framework.core.model.FormattedRMLMapping

/**
  * Test helper to read the mapping file and generate a FormattedRMLMapping
  */

object MappingTestUtil extends FileProcessingUtil[FormattedRMLMapping] {


  override def processFile(file: File): FormattedRMLMapping = {
    val mapping = MappingReader().read(file)
    FormattedRMLMapping.fromRMLMapping(mapping)
  }

  override def candidateFiles: List[String] = List("mapping.ttl", "mapping.rml.ttl", "example.rml.ttl")
}