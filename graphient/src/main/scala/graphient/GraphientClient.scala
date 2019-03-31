package graphient

import cats.implicits._
import cats.ApplicativeError
import io.circe.parser.decode
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import sangria.renderer.QueryRenderer
import sangria.schema.Schema

class GraphientClient[F[_]](
    schema:         Schema[_, _],
    endpoint:       Uri
)(implicit backend: SttpBackend[F, _], effect: cats.MonadError[F, Throwable]) {

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

  def callAndDecode[P: Encoder, T: Decoder](
      call:      GraphqlCall[_, _],
      variables: P
  ): F[Either[List[GraphqlResponseError], T]] = {
    for {
      request <- this.call(call, variables)
      rawResponse <- request.send()
      response <- rawResponse.body.flatMap(decode[RawGraphqlResponse[T]](_).bimap(_.toString, identity)) match {
        case Left(error) => effect.raiseError(new Exception(error)) // TODO: Better error type
        case Right(response) =>
          (response.errors, response.data) match {
            case (None, None) =>
              effect.raiseError(new Exception("Inconsistent response (no data, no errors)")) // TODO: Better error type
            case (Some(errors), _)  => effect.pure(Left(errors))
            case (None, Some(data)) => effect.pure(Right(data))
          }
      }
    } yield response
  }
}
