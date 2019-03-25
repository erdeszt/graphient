package graphient

import io.circe.{Encoder, Json}

object Instances {

  private def unsafe(x: Option[Json], `type`: String): Json = {
    x.getOrElse(throw new Exception(s"Invalid ${`type`} value"))
  }

  private def convertValue(value: Any): Json = {
    value match {
      case str:    String => Json.fromString(str)
      case float:  Float => unsafe(Json.fromFloat(float), "float")
      case double: Double => unsafe(Json.fromDouble(double), "double")
      case int:    Int => Json.fromInt(int)
      case long:   Long => Json.fromLong(long)
      case option: Option[_] =>
        option match {
          case None             => Json.Null
          case Some(innerValue) => convertValue(innerValue)
        }
      case list: List[_] =>
        Json.arr(list.map(convertValue): _*)
      case array: Array[_] =>
        Json.arr(array.map(convertValue): _*)
      case obj: Map[String, _] =>
        Json.obj(obj.toList.map { case (k, v) => (k, convertValue(v)) }: _*)

    }
  }

  implicit val mapStringAnyEncoder: Encoder[Map[String, Any]] = { values =>
    convertValue(values)
  }

}
