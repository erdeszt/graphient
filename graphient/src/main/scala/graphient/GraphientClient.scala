package graphient

import cats.effect.Sync
import cats.implicits._
import io.circe.parser.decode
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import sangria.renderer.QueryRenderer
import sangria.schema.Schema

class GraphientClient[F[_]](
    schema:         Schema[_, _],
    endpoint:       Uri
)(implicit backend: SttpBackend[F, _], effect: Sync[F]) {

  private val queryGenerator = new QueryGenerator(schema)

  def request[P: Encoder](call: GraphqlCall[_, _],
                          variables: P,
                          headers:   Map[String, String]): F[Request[String, Nothing]] = {
    queryGenerator.generateQuery(call) match {
      case Left(error) => effect.raiseError(error)
      case Right(query) =>
        val renderedQuery = QueryRenderer.render(query)
        val payload       = GraphqlRequest(renderedQuery, variables)

        effect.delay(
          sttp
            .body(payload)
            .contentType("application/json")
            .post(endpoint)
            .headers(headers)
        )
    }
  }

  def requestAndDecode[P: Encoder, T: Decoder](
      call:             GraphqlCall[_, _],
      variables:        P,
      transformRequest: Request[String, Nothing] => Request[String, Nothing] = identity
  ): F[Either[List[GraphqlResponseError], T]] = {
    for {
      request <- this.request(call, variables, Map.empty)
      rawResponse <- transformRequest(request).send()
      rawResponseBody     = rawResponse.body.bimap(GraphqlClientError, identity)
      decodedResponseBody = rawResponseBody.flatMap(decode[RawGraphqlResponse[T]])
      response <- decodedResponseBody match {
        case Left(error) => effect.raiseError[Either[List[GraphqlResponseError], T]](error)
        case Right(response) =>
          (response.errors, response.data) match {
            case (None, None) =>
              effect.raiseError[Either[List[GraphqlResponseError], T]](
                GraphqlClientError("Inconsistent response (no data, no errors)"))
            case (Some(errors), _)  => effect.pure(Left(errors): Either[List[GraphqlResponseError], T])
            case (None, Some(data)) => effect.pure(Right(data):  Either[List[GraphqlResponseError], T])
          }
      }
    } yield response
  }
}
