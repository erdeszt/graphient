package graphient

import cats.effect.Sync
import cats.implicits._
import io.circe.parser.decode
import com.softwaremill.sttp._
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor}
import sangria.renderer.QueryRenderer
import sangria.schema.Schema

class GraphientClient(schema: Schema[_, _], endpoint: Uri) {

  private val queryGenerator = new QueryGenerator(schema)

  def createRequest[P: Encoder](
      call:      GraphqlCall[_, _],
      variables: P,
      headers:   (String, String)*
  ): Either[GraphqlCallError, Request[String, Nothing]] = {
    queryGenerator.generateQuery(call).map { query =>
      val renderedQuery = QueryRenderer.render(query)
      val payload       = GraphqlRequest(renderedQuery, variables)

      sttp
        .body(payload)
        .contentType("application/json")
        .post(endpoint)
        .headers(headers.toMap)
    }
  }

  case class DataWrapper[T](fieldName: String, value: T)

  def dataWrapperDecoder[T: Decoder](fieldName: String): Decoder[DataWrapper[T]] = new Decoder[DataWrapper[T]] {
    def apply(c: HCursor): Result[DataWrapper[T]] = {
      val field   = c.downField(fieldName)
      val payload = implicitly[Decoder[T]].tryDecode(field)

      payload.map(DataWrapper(fieldName, _))
    }
  }

  def call[F[_], P: Encoder, T: Decoder](
      call:           GraphqlCall[_, _],
      variables:      P,
      headers:        (String, String)*
  )(implicit backend: SttpBackend[F, _], effect: Sync[F]): F[T] = {
    for {
      request <- createRequest(call, variables, headers: _*) match {
        case Left(error)         => effect.raiseError[Request[String, Nothing]](error)
        case Right(requestValue) => effect.pure(requestValue)
      }
      rawResponse <- request.send()
      rawResponseBody = rawResponse.body.leftMap(GraphqlClientError)
      decodedResponseBody = rawResponseBody.flatMap { body =>
        decode[RawGraphqlResponse[DataWrapper[T]]](body)(
          RawGraphqlResponse.graphqlResponseDecoder(dataWrapperDecoder[T](call.field.name))
        )
      }
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
            case (None, Some(data)) => effect.pure(data.value)
          }
      }
    } yield response
  }
}
