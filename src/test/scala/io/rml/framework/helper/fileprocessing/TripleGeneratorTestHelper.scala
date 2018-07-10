package io.rml.framework.helper.fileprocessing

import java.io.File

import io.rml.framework.Main
import io.rml.framework.core.extractors.MappingReader
import io.rml.framework.core.model.FormattedRMLMapping
import io.rml.framework.helper.Logger
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

/**
  * Test helper to get generated triples after processing the mapping file
  * containing triple maps.
  *
  */
object TripleGeneratorTestHelper extends TestFilesHelper[List[String]] {
  implicit val env = ExecutionEnvironment.getExecutionEnvironment
  implicit val senv = StreamExecutionEnvironment.getExecutionEnvironment


  override def getHelperSpecificFiles(path: String): Array[File] = {
    MappingTestHelper.getHelperSpecificFiles(path)
  }


  /**
    * Process the given mapping file and return a list of string containing triples generated
    * by the mapping process.
    *
    * @param file file containing triple maps
    * @return list of triples generated by processing the mapping file
    */
  override def processFile(file: File): List[String] = {
    val mapping = MappingReader().read(file)

    val formattedMapping = FormattedRMLMapping.fromRMLMapping(mapping)
    val dataSet = Main.createDataSetFromFormattedMapping(formattedMapping).collect

    val result = if (dataSet.nonEmpty) dataSet.reduce((a, b) => a + "\n" + b) else ""
    Logger.logInfo("Input file: "  +  file)
    Logger.logInfo("Result from processing: " + result.length)
    if (result.nonEmpty) {
      result.split('\n').toList
    } else {
      List()
    }
  }
}