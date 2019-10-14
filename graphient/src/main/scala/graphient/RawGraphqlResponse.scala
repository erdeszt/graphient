package graphient

import io.circe.Decoder
import io.circe.generic.semiauto._

case class RawGraphqlResponse[T](
    data:   Option[Map[String, T]],
    errors: Option[List[GraphqlResponseError]]
)

object RawGraphqlResponse {

  implicit def graphqlResponse2Decoder[T: Decoder] = deriveDecoder[RawGraphqlResponse[T]]

}
