package graphient

case class GraphqlClientError(message: String) extends Exception(message)
