package graphient

import io.circe
import io.circe.generic.semiauto._

case class GraphqlRequest[T](query: String, variables: T)

object GraphqlRequest {

  implicit def queryRequestEncoder[T: circe.Encoder] = deriveEncoder[GraphqlRequest[T]]

}
