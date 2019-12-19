package graphient

import sangria.schema.{Argument, Field}

object model {

  sealed trait GraphqlCall[Ctx, T] {
    val field: Field[Ctx, T]
  }
  final case class Query[Ctx, T](field:    Field[Ctx, T]) extends GraphqlCall[Ctx, T]
  final case class Mutation[Ctx, T](field: Field[Ctx, T]) extends GraphqlCall[Ctx, T]

  sealed trait NamedGraphqlCall {
    val field: String
  }
  final case class QueryByName(field:    String) extends NamedGraphqlCall
  final case class MutationByName(field: String) extends NamedGraphqlCall

  sealed trait GraphqlCallError extends Throwable
  final case class FieldNotFound(graphqlCall:         NamedGraphqlCall) extends GraphqlCallError
  final case class ArgumentNotFound[T](argument:      Argument[T]) extends GraphqlCallError
  final case class ArgumentFieldNotFound[T](argument: Argument[T], field: String) extends GraphqlCallError
  final case class InvalidArgumentValue[T](argument:  Argument[T], value: Any) extends GraphqlCallError

  sealed trait GraphqlClientError extends Throwable
  final case class InvalidResponseBody(invalidBody:            String) extends GraphqlClientError
  final case class InconsistentResponseNoDataNoError(response: RawGraphqlResponse[_]) extends GraphqlClientError
  final case class InconsistentResponseEmptyError(response:    RawGraphqlResponse[_]) extends GraphqlClientError
  final case class InconsistentResponseNoData(response:        RawGraphqlResponse[_]) extends GraphqlClientError

  final case class GraphqlRequest[T](query: String, variables: T)

  final case class RawGraphqlResponse[T](
      data:   Option[Map[String, T]],
      errors: Option[List[GraphqlResponseError]]
  )

  final case class GraphqlResponseError(
      message:   String,
      path:      List[String],
      locations: List[GraphqlResponseErrorLocation]
  ) extends Throwable

  final case class GraphqlResponseErrorLocation(line: Int, column: Int)

}
