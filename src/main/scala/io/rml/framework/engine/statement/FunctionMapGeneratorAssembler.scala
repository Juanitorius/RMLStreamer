/**
 * MIT License
 *
 * Copyright (C) 2017 - 2020 RDF Mapping Language (RML)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 **/
package io.rml.framework.engine.statement

import java.io.File
import io.rml.framework.api.RMLEnvironment
import io.rml.framework.core.function.FunctionLoader
import io.rml.framework.core.function.model.Function
import io.rml.framework.core.model._
import io.rml.framework.core.vocabulary.RMLVoc
import io.rml.framework.flink.item.Item
import io.rml.framework.flink.sink.FlinkRDFQuad
import io.rml.framework.flink.source.EmptyItem
import io.rml.framework.shared.RMLException

case class FunctionMapGeneratorAssembler() extends TermMapGeneratorAssembler {


  override def assemble(termMap: TermMap): (Item) => Option[Iterable[Entity]] = {
    require(termMap.isInstanceOf[FunctionMap], "Wrong TermMap instance.")

    val functionMap = termMap.asInstanceOf[FunctionMap]
    val pomAssembler = PredicateObjectGeneratorAssembler()

    val assembledPom = functionMap.functionValue
      .flatMap(pomAssembler.assemble)
      .map {
        case (predicateGen, objGen, _) => (predicateGen, objGen)
      }

    val function = parseFunction(assembledPom)

    createAssemblerFunction(function, assembledPom)
  }

  private def parseFunction(assembledPom:
                            List[(Item => Option[Iterable[Uri]], Item => Option[Iterable[Entity]])]): Function = {

    this.logDebug("parseFunction (assembledPom)")
    val placeHolder: List[FlinkRDFQuad] = generateFunctionTriples(new EmptyItem(), assembledPom)

    val executeProperties = placeHolder.filter( quad => quad.predicate.value == Uri(RMLVoc.Property.EXECUTES))
    if(executeProperties.isEmpty)
      throw new RMLException(s"Couldn't find ${RMLVoc.Property.EXECUTES} property." +
        s"Is the namespace correct? (e.g. HTTP vs. HTTPS)")

    val functionName = Uri(
      executeProperties
      .head
      .`object`
      .value
      .toString)
    
    val loadedFunctionOption = FunctionLoader().loadFunction(functionName)
    loadedFunctionOption.getOrElse{
      // complain about function that isn't present
      throw new RMLException("Can't load function..")
    }
  }

  /**
   * Generates an assembler function which takes in [[Item]] and generate
   * entities using the function specified by the function map
   *
   * @param assembledPom List of predicate object generator functions
   * @return anon function taking in [[Item]] and returns entities using the function
   */
  private def createAssemblerFunction(function: Function, assembledPom: List[(Item => Option[Iterable[Uri]], Item => Option[Iterable[Entity]])]): Item => Option[Iterable[Entity]] = {
    (item: Item) => {
      val triples: List[FlinkRDFQuad] = generateFunctionTriples(item, assembledPom)
      val args: Map[Uri, String] = triples.filter(triple => triple.predicate.uri != Uri(RMLVoc.Property.EXECUTES))
        .map(triple => {
          val parameterName = triple.predicate.uri
          val parameterValue = triple.`object`.value.toString
          parameterName -> parameterValue
        })
        .toMap
      function.initialize()
      function.execute(args)
    }
  }

  /**
   * Generate triples from which the the function can be derived from
   * and applied to the the item
   *
   * @param item
   * @return
   */
  private def generateFunctionTriples(item: Item, assembledPom: List[(Item => Option[Iterable[Uri]], Item => Option[Iterable[Entity]])]): List[FlinkRDFQuad] = {

    val result = for{
      (predicateGen, objGen) <- assembledPom
      predicateIter <- predicateGen(item)
      objIter <- objGen(item)
    } yield for {
      predicate <- predicateIter
      obj <- objIter
      quad <-  Statement.generateQuad(Blank(), predicate, obj)
    } yield quad


    result.flatten

  }

}
