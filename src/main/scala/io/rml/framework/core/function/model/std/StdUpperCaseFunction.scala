package io.rml.framework.core.function.model.std

import java.lang.reflect.Method

import io.rml.framework.core.function.model.Function
import io.rml.framework.core.model.{Entity, Literal, Uri}
import io.rml.framework.core.vocabulary.{Namespaces, RMLVoc}

case class StdUpperCaseFunction(identifier: String = RMLVoc.Property.GREL_UPPERCASE) extends Function {
  override def execute(arguments: Map[Uri, String]): Option[Iterable[Entity]] = {
    val parameter = arguments.get(Uri(Namespaces("grel", "valueParameter")))

    parameter.map(string => List(Literal(string)))
  }

  override def getMethod: Option[Method] = {
    None
  }
}
