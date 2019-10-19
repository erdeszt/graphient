package graphient

case class GraphqlRequest[T](query: String, variables: T)
