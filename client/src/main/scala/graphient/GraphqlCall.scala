package graphient

import sangria.schema._

object GraphqlCall {

  sealed trait GraphqlCall {
    val field: String
  }
  case class Query(field:    String) extends GraphqlCall
  case class Mutation(field: String) extends GraphqlCall

  sealed trait GraphqlCallError
  case class FieldNotFound(graphqlCall:        GraphqlCall) extends GraphqlCallError
  case class ArgumentNotFound[T](argument:     Argument[T]) extends GraphqlCallError
  case class InvalidArgumentValue[T](argument: Argument[T], value: Any) extends GraphqlCallError

}
