package graphient

import cats.effect.Async
import com.softwaremill.sttp._
import io.circe.Encoder
import sangria.renderer.QueryRenderer
import sangria.schema.Schema

class GraphientClient[F[_]](
    schema:         Schema[_, _],
    endpoint:       Uri
)(implicit backend: SttpBackend[F, _], effect: Async[F]) {

  private val queryGenerator = new QueryGenerator(schema)

  def call[P: Encoder](call: GraphqlCall[_, _], variables: P): F[Response[String]] = {
    queryGenerator.generateQuery(call) match {
      case Left(error) => effect.raiseError(error)
      case Right(query) =>
        val renderedQuery = QueryRenderer.render(query)
        val payload       = GraphqlRequest(renderedQuery, variables)

        sttp
          .body(payload)
          .post(endpoint)
          .send()
    }

  }
}
