package graphient

import io.circe.generic.semiauto._

case class GraphqlResponseError(message: String, path: List[String], locations: List[GraphqlResponseErrorLocation])
    extends Throwable

object GraphqlResponseError {

  implicit val graphqlResponseErrorDecoder = deriveDecoder[GraphqlResponseError]

}
