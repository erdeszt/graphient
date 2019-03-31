package graphient

case class GraphqlResponseError(message: String, path: List[String], locations: List[GraphqlResponseErrorLocation])
