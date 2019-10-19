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

  implicit val mapOfStringToAnySprayWriter: JsonFormat[Map[String, Any]] = new JsonFormat[Map[String, Any]] {
    def write(obj: Map[String, Any]): JsValue = {
      // TODO
      ???
    }

    def read(json: JsValue): Map[String, Any] = {
      json.convertTo[Map[String, Any]]
    }
  }

  implicit def graphqlRequestSprayFormat[T: JsonFormat] = jsonFormat2(GraphqlRequest[T](_, _))
  implicit val graphqlResponseErrorLocationSprayForamt = jsonFormat2(GraphqlResponseErrorLocation(_, _))
  implicit val graphqlResponseErrorSprayFormat         = jsonFormat3(GraphqlResponseError(_, _, _))
  implicit def rawGraphqlResponseSprayFormat[T: JsonFormat] = jsonFormat2(RawGraphqlResponse[T](_, _))

}
