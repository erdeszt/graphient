package graphienttp
import com.softwaremill.sttp.{sttp, BodySerializer, Id, Request, Response, StringBody, SttpBackend, Uri}
import graphient._
import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.syntax._
import sangria.renderer.QueryRenderer
import sangria.schema.Schema

case class QueryRequest[T](query: String, variables: T)
object QueryRequest {
  implicit val config = Configuration.default

  implicit def queryRequestEncoder[T: Encoder] = deriveEncoder[QueryRequest[T]]

  implicit def queryRequestSerializer[T: Encoder]: BodySerializer[QueryRequest[T]] = { tokenRequest =>
    val serialized = tokenRequest.asJson.noSpaces

    StringBody(serialized, "UTF-8", Some("application/json"))
  }
}

// TODO: Generalize the effect type
class GraphienttpClient[S](schema: Schema[S, Unit], endpoint: Uri)(implicit backend: SttpBackend[Id, Nothing]) {

  val queryGenerator = new QueryGenerator(schema)

  def runQuery[T, P: Encoder](query: Query[S, T], variables: P): Id[Response[String]] = {
    val q     = queryGenerator.generateQuery(query) // TODO: work with either, put into for comprehension
    val qJson = QueryRenderer.render(q.right.get)

    val payload = QueryRequest(qJson, variables)

    val request = sttp
      .body(payload)
      .post(endpoint)

    request.send()
  }

  def runMutation() = {}

}
