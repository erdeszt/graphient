package graphient.serializer

import graphient.model._
import _root_.spray.json._
import scala.util.Try

object spray extends DefaultJsonProtocol {

  implicit def sprayEncoder[T](implicit jsonFormat: JsonWriter[T]): Encoder[T] = {
    { requestBody: T =>
      jsonFormat.write(requestBody).compactPrint
    }
  }

  implicit def sprayDecoder[T](implicit jsonFormat: JsonReader[T]): Decoder[T] = {
    { responseBody: String =>
      Try(jsonFormat.read(responseBody.parseJson)).toEither
    }
  }

  private def convertValue(value: Any): JsValue = {
    value match {
      case str: String =>
        JsString(str)
      case float: Float =>
        JsNumber(float.toDouble)
      case double: Double =>
        JsNumber(double)
      case int: Int =>
        JsNumber(int)
      case bigInt: BigInt =>
        JsNumber(bigInt)
      case long: Long =>
        JsNumber(long)
      case option: Option[_] =>
        option match {
          case None             => JsNull
          case Some(innerValue) => convertValue(innerValue)
        }
      case list: List[_] =>
        JsArray(list.map(convertValue).toVector)
      case array: Array[_] =>
        JsArray(array.map(convertValue).toVector)
      case obj: Map[_, _] =>
        val mappedValues = obj.mapValues(convertValue).asInstanceOf[Map[String, JsValue]]
        JsObject(mappedValues)
    }
  }

  implicit def graphqlRequestSprayFormat[T: JsonFormat] = jsonFormat2(GraphqlRequest[T](_, _))
  implicit val graphqlResponseErrorLocationSprayForamt = jsonFormat2(GraphqlResponseErrorLocation(_, _))
  implicit val graphqlResponseErrorSprayFormat         = jsonFormat3(GraphqlResponseError(_, _, _))
  implicit def rawGraphqlResponseSprayFormat[T: JsonFormat] = jsonFormat2(RawGraphqlResponse[T](_, _))
  implicit val mapOfStringToAnySprayEncoder = new JsonFormat[Map[String, Any]] {
    def write(obj: Map[String, Any]): JsValue = {
      JsObject(obj.mapValues(convertValue))
    }

    def read(json: JsValue): Map[String, Any] = {
      ???
    }
  }

}
