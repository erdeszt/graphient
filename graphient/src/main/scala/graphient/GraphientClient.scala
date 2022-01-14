package graphient

import sttp.client3._
import cats.syntax.flatMap._
import cats.syntax.either._
import cats.syntax.functor._
import graphient.model._
import graphient.serializer._
import sangria.renderer.QueryRenderer
import sangria.schema.Schema
import sttp.model.{MediaType, Uri}

class GraphientClient(schema: Schema[_, _], endpoint: Uri) {

  private val queryGenerator = new QueryGenerator(schema)

  def createRequest[P](
      call:           GraphqlCall[_, _],
      variables:      P,
      headers:        (String, String)*
  )(implicit encoder: Encoder[GraphqlRequest[P]])
    : Either[GraphqlCallError, Request[Either[String, String], Any]] = {
    queryGenerator.generateQuery(call).map { query =>
      val renderedQuery = QueryRenderer.render(query)
      val payload       = GraphqlRequest(renderedQuery, variables)
      val body          = StringBody(encoder.encode(payload), "UTF-8", MediaType.ApplicationJson)

      basicRequest
        .body(body)
        .contentType("application/json")
        .post(endpoint)
        .headers(headers.toMap)
    }
  }

  def call[T]: CallBuilder[T] = {
    new CallBuilder[T]
  }

  final class CallBuilder[T] {
    def apply[F[_], P](
        graphqlCall: GraphqlCall[_, _],
        variables:   P,
        headers:     (String, String)*
    )(
        implicit
        backend: SttpBackend[F, _],
        effect:  cats.MonadError[F, Throwable],
        encoder: Encoder[GraphqlRequest[P]],
        decoder: Decoder[RawGraphqlResponse[T]]
    ): F[T] = {
      call[F, P, T](graphqlCall, variables, headers: _*)
    }
  }

  def call[F[_], P, T](
      call:      GraphqlCall[_, _],
      variables: P,
      headers:   (String, String)*
  )(
      implicit
      backend: SttpBackend[F, _],
      effect:  cats.MonadError[F, Throwable],
      encoder: Encoder[GraphqlRequest[P]],
      decoder: Decoder[RawGraphqlResponse[T]]
  ): F[T] = {
    for {
      request <- effect.fromEither(createRequest(call, variables, headers: _*))
      rawResponse <- request.send(backend)
      rawResponseBody <- effect.fromEither(rawResponse.body.leftMap(InvalidResponseBody))
      decodedResponse <- effect.fromEither(decoder.decode(rawResponseBody))
      responseData <- (decodedResponse.errors, decodedResponse.data) match {
        case (None, None) =>
          effect.raiseError[Map[String, T]](InconsistentResponseNoDataNoError(decodedResponse))
        case (Some(Nil), _)             => effect.raiseError[Map[String, T]](InconsistentResponseEmptyError(decodedResponse))
        case (Some(firstError :: _), _) => effect.raiseError[Map[String, T]](firstError)
        case (None, Some(data))         => effect.pure(data)
      }
      response <- responseData.get(call.field.name) match {
        case None               => effect.raiseError[T](InconsistentResponseNoData(decodedResponse))
        case Some(callResponse) => effect.pure(callResponse)
      }
    } yield response
  }

}
