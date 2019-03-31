package graphient

import cats.ApplicativeError
import com.softwaremill.sttp._
import io.circe.Encoder
import sangria.renderer.QueryRenderer
import sangria.schema.Schema

class GraphientClient[F[_]](
    schema:         Schema[_, _],
    endpoint:       Uri
)(implicit backend: SttpBackend[F, _], effect: ApplicativeError[F, Throwable]) {

  private val queryGenerator = new QueryGenerator(schema)

  def call[P: Encoder](call: GraphqlCall[_, _], variables: P): F[RequestT[Id, String, Nothing]] = {
    queryGenerator.generateQuery(call) match {
      case Left(error) => effect.raiseError(error)
      case Right(query) =>
        val renderedQuery = QueryRenderer.render(query)
        val payload       = GraphqlRequest(renderedQuery, variables)

        effect.pure(
          sttp
            .body(payload)
            .contentType("application/json")
            .post(endpoint)
        )
    }

  }
}
