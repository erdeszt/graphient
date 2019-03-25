package graphient

import com.softwaremill.sttp.{BodySerializer, StringBody}
import io.circe.Encoder
import io.circe.syntax._
import io.circe.generic.semiauto._

private[graphient] case class GraphqlRequest[T](query: String, variables: T)

private[graphient] object GraphqlRequest {

  implicit def queryRequestEncoder[T: Encoder] = deriveEncoder[GraphqlRequest[T]]

  implicit def queryRequestSerializer[T: Encoder]: BodySerializer[GraphqlRequest[T]] = { tokenRequest =>
    val serialized = tokenRequest.asJson.noSpaces

    StringBody(serialized, "UTF-8", Some("application/json"))
  }
}
