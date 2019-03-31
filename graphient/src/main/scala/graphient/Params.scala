package graphient

object Params {

  def apply(values: (String, Any)*): Map[String, Any] = values.toMap

}
