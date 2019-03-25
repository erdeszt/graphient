package graphient

import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.semiauto._

case class GetUserPayload(userId: Long)
object GetUserPayload {
  implicit val config = Configuration.default
  implicit val gupEncoder: Encoder[GetUserPayload] = deriveEncoder[GetUserPayload]
}
