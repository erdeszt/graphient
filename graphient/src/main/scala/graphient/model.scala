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

  sealed abstract class GraphqlCallError(message: String) extends RuntimeException(message)
  final case class FieldNotFound(graphqlCall:     NamedGraphqlCall)
      extends GraphqlCallError(s"Could not find field: ${graphqlCall.field}")
  final case class ArgumentNotFound[T](argument: Argument[T])
      extends GraphqlCallError(s"Argument not found: ${argument.name}")
  final case class ArgumentFieldNotFound[T](argument: Argument[T], field: String)
      extends GraphqlCallError(s"Argument field not found: ${argument.name} - ${field}")
  final case class InvalidArgumentValue[T](argument: Argument[T], value: Any)
      extends GraphqlCallError(s"Invalid value fort argument `${argument.name}`: ${value}")

  sealed abstract class GraphqlClientError(message: String) extends RuntimeException(message)
  final case class InvalidResponseBody(invalidBody: String)
      extends GraphqlClientError(s"Response body is invalid: ${invalidBody}")
  final case class InconsistentResponseNoDataNoError(response: RawGraphqlResponse[_])
      extends GraphqlClientError(s"No data or error field in the graphql response.")
  final case class InconsistentResponseEmptyError(response: RawGraphqlResponse[_])
      extends GraphqlClientError("No error field in the graphql response.")
  final case class InconsistentResponseNoData(response: RawGraphqlResponse[_])
      extends GraphqlClientError("No data field in the graphql response")

  final case class GraphqlRequest[T](query: String, variables: T)

  final case class RawGraphqlResponse[T](
      data:   Option[Map[String, T]],
      errors: Option[List[GraphqlResponseError]]
  )

  final case class GraphqlResponseError(
      message:   String,
      path:      List[String],
      locations: List[GraphqlResponseErrorLocation]
  ) extends RuntimeException(
        s"Response error: ${message} at: ${locations.mkString(";")}, path: ${path.mkString(";")}"
      )

  final case class GraphqlResponseErrorLocation(line: Int, column: Int)

}
