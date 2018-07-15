import sangria.schema.{Argument, Field}

package object graphient {

  sealed trait GraphqlCall {
    val field: String
  }
  case class Query(field:    String) extends GraphqlCall
  case class Mutation(field: String) extends GraphqlCall

  sealed trait GraphqlCallError
  case class FieldNotFound(graphqlCall:        GraphqlCall) extends GraphqlCallError
  case class ArgumentNotFound[T](argument:     Argument[T]) extends GraphqlCallError
  case class InvalidArgumentValue[T](argument: Argument[T], value: Any) extends GraphqlCallError

  sealed trait GraphqlCallV2[Ctx, T] {
    val field: Field[Ctx, T]
  }
  case class QueryV2[Ctx, T](field:    Field[Ctx, T]) extends GraphqlCallV2[Ctx, T]
  case class MutationV2[Ctx, T](field: Field[Ctx, T]) extends GraphqlCallV2[Ctx, T]

}
