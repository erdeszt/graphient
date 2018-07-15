package graphient

import sangria.ast
import sangria.schema._
import graphient.GraphqlCall._

// TODO: Add variable generator api using dynamic for POC, shapeless later
object ClientV2 {

  case class QueryGenerator[C, T](schema: Schema[C, T]) {

    def generateQuery(call: GraphqlCall): Either[GraphqlCallError, ast.Document] = {
      val (fieldLookup, operationType) = call match {
        case Query(_)    => (schema.query.fieldsByName, ast.OperationType.Query)
        case Mutation(_) => (schema.mutation.map(_.fieldsByName).getOrElse(Map()), ast.OperationType.Mutation)
      }

      fieldLookup.get(call.field) match {
        case None => Left(FieldNotFound(call))
        case Some(fields) =>
          fields.toList match {
            case Nil => Left(FieldNotFound(call))
            case field :: _ =>
              val argumentsAst = generateArgumentListAst(field.arguments)
              generateSelectionAst(field.fieldType).map(wrapInDocument(operationType, field, argumentsAst, _))
          }
      }

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

    private def generateSelectionAst[R](outputType: OutputType[R]): Either[GraphqlCallError, Vector[ast.Selection]] = {
      outputType match {
        case obj: ObjectType[_, _] =>
          val fieldAsts = obj.fields.map { field =>
            ast.Field(
              alias      = None,
              name       = field.name,
              arguments  = Vector(),
              directives = Vector(),
              selections = Vector()
            )
          }
          Right(Vector(fieldAsts: _*))
        case opt: OptionType[_] =>
          generateSelectionAst(opt.ofType)
        case _ => Left(UnsuportedOutputType(outputType))
      }
    }

    private def generateArgumentListAst[Ctx, R](arguments: List[Argument[_]]): Vector[ast.Argument] = {
      val argumentAsts =
        arguments.map { argument =>
          ast.Argument(argument.name, ast.VariableValue(argument.name))
        }
      Vector(argumentAsts: _*)
    }

  }
}
