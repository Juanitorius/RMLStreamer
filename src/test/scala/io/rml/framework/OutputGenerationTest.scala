package io.rml.framework

import java.io.File

import io.rml.framework.helper.fileprocessing.{OutputTestHelper, TripleGeneratorTestHelper}
import io.rml.framework.helper.{Logger, Sanitizer}
import io.rml.framework.shared.{RMLException, TermTypeException}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Sorting
import scala.util.control.Exception


class OutputGenerationTest extends FlatSpec with Matchers {

  val failing = "failing_test_cases/liter_typecast_fail"
  val passing = "rml-testcases"
  "Output from the generator" should "match the output from ouput.ttl" in {
    test(passing, checkGeneratedOutput)

  }

  it should "throw TripleTypeException if the termType of the subject is a Literal" in {
    
    assertThrows[TermTypeException] {
      test(failing, checkForTermTypeException)
      throw new TermTypeException("")
    }
  }




  def checkForTermTypeException(testFolderPath: String): Unit = {
    val catcher = Exception.catching(classOf[TermTypeException])
    val eitherGenerated = catcher.either(TripleGeneratorTestHelper.processFilesInTestFolder(testFolderPath).flatten)


    if (eitherGenerated.isRight) {
      val generatedOutput = Sanitizer.sanitize(eitherGenerated.right.get)
      Logger.logInfo("Generated output: \n" + generatedOutput.mkString("\n"))
      fail
    }
  }


  /**
    * Check and match the generated output with the expected output
    *
    * @param testFolderPath
    */
  def checkGeneratedOutput(testFolderPath: String): Unit = {
    var expectedOutputs: Set[String] = OutputTestHelper.processFilesInTestFolder(testFolderPath).toSet.flatten
    var generatedOutputs: List[String] = TripleGeneratorTestHelper.processFilesInTestFolder(testFolderPath).flatten

    /**
      * The amount of spaces added in the generated triples might be different from the expected triple.
      * Sanitization of the sequences will be done here.
      */

    expectedOutputs = Sanitizer.sanitize(expectedOutputs)
    generatedOutputs = Sanitizer.sanitize(generatedOutputs)

    Logger.logInfo("Generated output: \n " + generatedOutputs.mkString("\n"))
    Logger.logInfo("Expected Output: \n " + expectedOutputs.mkString("\n"))


    /**
      * Check if the generated triple is in the expected output.
      */

    Logger.logInfo("Generated size: " + generatedOutputs.size)

    for (generatedTriple <- generatedOutputs) {

      val errorMsgMismatch = Array("Generated output does not match expected output",
        "Expected: \n" + expectedOutputs.mkString("\n"),
        "Generated: \n" + generatedOutputs.mkString("\n")).mkString("\n")


      assert(expectedOutputs.contains(generatedTriple), errorMsgMismatch)
    }
  }


  def test(rootDir: String, checkFunc: String => Unit): Unit = {
    var checkedTestCases = Array("")
    for (pathString <- OutputTestHelper.getTestCaseFolders(rootDir).map(_.toString)) {

      checkFunc(pathString)
      val testCase = new File(pathString).getName
      Logger.logSuccess("Passed processing: " + testCase)
      Logger.lineBreak()
      checkedTestCases :+= testCase
    }

    Sorting.quickSort(checkedTestCases)
    Logger.logInfo("Processed test cases: " + checkedTestCases.mkString("\n"))
    Logger.lineBreak()
  }

}