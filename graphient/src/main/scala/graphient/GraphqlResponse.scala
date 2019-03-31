package graphient

import io.circe.Decoder
import io.circe.generic.semiauto._

case class GraphqlResponse[T](data: T, errors: Option[List[GraphqlResponseError]])

object GraphqlResponse {

  implicit val graphqlResponseErrorDecoder = deriveDecoder[GraphqlResponseError]

  implicit def graphqlResponseDecoder[T: Decoder] = deriveDecoder[GraphqlResponse[T]]

}
