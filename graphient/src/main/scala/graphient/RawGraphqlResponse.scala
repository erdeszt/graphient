package graphient

import io.circe.Decoder
import io.circe.generic.semiauto._

private[graphient] case class RawGraphqlResponse[T](data: Option[T], errors: Option[List[GraphqlResponseError]])

private[graphient] object RawGraphqlResponse {

  implicit val graphqlResponseErrorLocationDecoder = deriveDecoder[GraphqlResponseErrorLocation]

  implicit val graphqlResponseErrorDecoder = deriveDecoder[GraphqlResponseError]

  implicit def graphqlResponseDecoder[T: Decoder] = deriveDecoder[RawGraphqlResponse[T]]

}
