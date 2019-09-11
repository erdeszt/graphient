package graphient

import cats.effect.Sync
import cats.implicits._
import io.circe.parser.decode
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import sangria.renderer.QueryRenderer
import sangria.schema.Schema

class GraphientClient(schema: Schema[_, _], endpoint: Uri) {

  private val queryGenerator = new QueryGenerator(schema)

  def createRequest[P: Encoder](
      call:      GraphqlCall[_, _],
      variables: P,
      headers:   Map[String, String] = Map.empty
  ): Either[GraphqlCallError, Request[String, Nothing]] = {
    queryGenerator.generateQuery(call).map { query =>
      val renderedQuery = QueryRenderer.render(query)
      val payload       = GraphqlRequest(renderedQuery, variables)

      sttp
        .body(payload)
        .contentType("application/json")
        .post(endpoint)
        .headers(headers)
    }
  }

  def call[F[_], P: Encoder, T: Decoder](
      call:             GraphqlCall[_, _],
      variables:        P,
      transformRequest: Request[String, Nothing] => Request[String, Nothing] = identity
  )(implicit backend:   SttpBackend[F, _], effect: Sync[F]): F[T] = {
    for {
      request <- createRequest(call, variables) match {
        case Left(error)         => effect.raiseError[Request[String, Nothing]](error)
        case Right(requestValue) => effect.pure(requestValue)
      }
      rawResponse <- transformRequest(request).send()
      rawResponseBody     = rawResponse.body.leftMap(GraphqlClientError)
      decodedResponseBody = rawResponseBody.flatMap(decode[RawGraphqlResponse[T]])
      response <- decodedResponseBody match {
        case Left(error) => effect.raiseError[T](error)
        case Right(response) =>
          (response.errors, response.data) match {
            case (None, None) =>
              effect.raiseError[T](GraphqlClientError("Inconsistent response (no data, no errors)"))
            case (Some(Nil), _) =>
              effect.raiseError[T](GraphqlClientError("Error fields is present but empty"))
            case (Some(firstError :: _), _) =>
              effect.raiseError[T](firstError)
            case (None, Some(data)) => effect.pure(data)
          }
      }
    } yield response
  }
}
