package graphient

import io.circe.generic.semiauto._

case class GraphqlResponseErrorLocation(line: Int, column: Int)

object GraphqlResponseErrorLocation {

  implicit val graphqlResponseErrorLocationDecoder = deriveDecoder[GraphqlResponseErrorLocation]

}
