package graphient

import io.circe

case class GraphqlRequest[T](query: String, variables: T)

object GraphqlRequest {

//  implicit def queryRequestEncoder[T: circe.Encoder] = deriveEncoder[GraphqlRequest[T]]

}
