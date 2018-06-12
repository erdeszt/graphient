package graphient

import sangria.ast
import sangria.schema._
import cats.implicits._

// TODO: Add stricter scalac flags

// TODO: Move these to separaete files
sealed trait GraphqlCall {
  val field: String
}
case class Query(field:    String) extends GraphqlCall
case class Mutation(field: String) extends GraphqlCall

sealed trait GraphqlCallError
case class FieldNotFound(graphqlCall:          GraphqlCall) extends GraphqlCallError
case class ArgumentNotFound[T](argument:       Argument[T]) extends GraphqlCallError
case class UnsuportedArgumentType[T](argument: Argument[T]) extends GraphqlCallError
case class UnsuportedOutputType[T](outputType: OutputType[T]) extends GraphqlCallError

case class Client[Ctx, R](schema: Schema[Ctx, R]) {

  def call(
      call:           GraphqlCall,
      argumentValues: Map[String, Any]
  ): Either[GraphqlCallError, ast.Document] = {
    val (fields, operationType) = call match {
      case Query(_)    => (schema.query.fieldsByName, ast.OperationType.Query)
      case Mutation(_) => (schema.mutation.map(_.fieldsByName).getOrElse(Map()), ast.OperationType.Mutation)
    }

    fields.get(call.field) match {
      case None                           => Left(FieldNotFound(call))
      case Some(fields) if fields.isEmpty => Left(FieldNotFound(call))
      case Some(fields)                   => generateGraphqlAst(operationType, fields.head, argumentValues)
    }
  }

  def generateGraphqlAst[Ctx, R](
      operationType: ast.OperationType,
      field:         Field[Ctx, R],
      arguments:     Map[String, Any]
  ): Either[GraphqlCallError, ast.Document] = {
    for {
      argumentsAst <- generateArgumentListAst(field.arguments, arguments)
      selectionsAst <- generateSelectionAst(field)
    } yield wrapInDocument(operationType, field, argumentsAst, selectionsAst)
  }

  private def wrapInDocument[Ctx, R](
      operationType: ast.OperationType,
      field:         Field[Ctx, R],
      arguments:     Vector[ast.Argument],
      selections:    Vector[ast.Selection]
  ): ast.Document = {
    ast.Document(
      Vector(
        ast.OperationDefinition(
          operationType = operationType,
          selections = Vector(
            ast.Field(
              alias      = None,
              name       = field.name,
              arguments  = arguments,
              directives = Vector(),
              selections = selections
            )
          )
        )
      )
    )

  }

  private def generateSelectionAst[Ctx, R](field: Field[Ctx, R]): Either[GraphqlCallError, Vector[ast.Selection]] = {
    field.fieldType match {
      case obj: ObjectType[_, _] =>
        val selections = obj.fields.map { field =>
          ast.Field(
            alias      = None,
            name       = field.name,
            arguments  = Vector(),
            directives = Vector(),
            selections = Vector()
          )
        }

        Right(Vector(selections: _*))
      case _ => Left(UnsuportedOutputType(field.fieldType))
    }
  }

  private def generateArgumentListAst[Ctx, R](
      arguments:      List[Argument[_]],
      argumentValues: Map[String, Any]
  ): Either[GraphqlCallError, Vector[ast.Argument]] = {
    arguments
      .map { argument =>
        argumentValues.get(argument.name) match {
          case None => Left(ArgumentNotFound(argument))
          case Some(argumentValue) =>
            argumentValueToAstValue(argument, argumentValue).map(ast.Argument(argument.name, _))
        }
      }
      .sequence[Either[GraphqlCallError, ?], ast.Argument]
      .map(argumentList => Vector(argumentList: _*))
  }

  private def argumentValueToAstValue(argument: Argument[_], value: Any): Either[GraphqlCallError, ast.Value] = {
    argument.argumentType match {
      // TODO: THIS IS UNCHECKED => FIX THE CHECK SOMEHOW
      case longValue: ScalarType[Long] => Right(ast.BigIntValue(BigInt(value.asInstanceOf[Long])))
      case _ => Left(UnsuportedArgumentType(argument))
    }
  }

}
