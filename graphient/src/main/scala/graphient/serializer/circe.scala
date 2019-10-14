package graphient.serializer

import io.circe.Json
import io.circe.syntax._

object circe {

  implicit def circeEncoder[T](implicit encoder: io.circe.Encoder[T]): Encoder[T] = {
    { value: T =>
      value.asJson.noSpaces
    }
  }

  implicit def circeDecoder[T](implicit decoder: io.circe.Decoder[T]): Decoder[T] = {
    { responseBody: String =>
      io.circe.parser.decode(responseBody)
    }
  }

  private def unsafe(x: Option[Json], `type`: String): Json = {
    x.getOrElse(throw new Exception(s"Invalid ${`type`} value"))
  }

  private def convertValue(value: Any): Json = {
    value match {
      case str: String =>
        Json.fromString(str)
      case float: Float =>
        unsafe(Json.fromFloat(float), "float")
      case double: Double =>
        unsafe(Json.fromDouble(double), "double")
      case int: Int =>
        Json.fromInt(int)
      case bigInt: BigInt =>
        Json.fromBigInt(bigInt)
      case long: Long =>
        Json.fromLong(long)
      case option: Option[_] =>
        option match {
          case None             => Json.Null
          case Some(innerValue) => convertValue(innerValue)
        }
      case list: List[_] =>
        Json.arr(list.map(convertValue): _*)
      case array: Array[_] =>
        Json.arr(array.map(convertValue): _*)
      case obj: Map[_, _] =>
        Json.obj(obj.toList.map { case (k, v) => (k.asInstanceOf[String], convertValue(v)) }: _*)

    }
  }

  implicit val mapOfStringToAnyCirceEncoder: io.circe.Encoder[Map[String, Any]] = convertValue

}
