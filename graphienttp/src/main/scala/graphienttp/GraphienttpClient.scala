package graphienttp
import com.softwaremill.sttp.{sttp, BodySerializer, Id, Request, StringBody, SttpBackend, Uri}
import graphient._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.syntax._
import sangria.marshalling.QueryAstInputUnmarshaller
import sangria.renderer.QueryRenderer
import sangria.schema.Schema

// TODO: Naive circe impl - sanity check ?
case class QueryRequest(query: String, variables: String)
object QueryRequest {
  implicit val config = Configuration.default

  implicit lazy val queryRequestEncoder = deriveEncoder[QueryRequest]

  implicit lazy val queryRequestSerializer: BodySerializer[QueryRequest] = { tokenRequest =>
    val serialized = tokenRequest.asJson.noSpaces

    StringBody(serialized, "UTF-8", Some("application/json"))
  }
}

class GraphienttpClient[S](schema: Schema[S, Unit], endpoint: Uri)(implicit backend: SttpBackend[Id, Nothing]) {

  val queryGenerator    = new QueryGenerator(schema)
  val variableGenerator = new VariableGenerator(schema)
  val unmarshaller      = new QueryAstInputUnmarshaller()

  def runQuery[T](query: Query[S, T], variables: Map[String, Any]): Request[String, Nothing] = {
    val q    = queryGenerator.generateQuery(query) // TODO: work with either
    val vars = variableGenerator.generateVariables(query.field, variables) // TODO

    val variableJson = unmarshaller.render(vars.right.get) // TODO: put into for comprehension
    val qJson        = QueryRenderer.render(q.right.get) // TODO: put into for comprehension

    // val payload = "pong" // TODO: how to create payload with circie ?

    val payload = QueryRequest(qJson, variableJson)

    val request = sttp
      .body(payload)
      .post(endpoint)

    request
  }

  def runMutation() = {}

}
