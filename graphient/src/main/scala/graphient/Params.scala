package graphient

object Params {

  type T = Map[String, Any]

  def apply(values: (String, Any)*): Map[String, Any] = values.toMap

}
