package graphient

import sangria.ast
import sangria.schema._
import graphient.GraphqlCall._

// TODO: Add variable generator api using dynamic for POC, shapeless later
object ClientV2 {

  sealed trait GraphqlCallV2[Ctx, T] {
    val field: Field[Ctx, T]
  }
  case class QueryV2[Ctx, T](field:    Field[Ctx, T]) extends GraphqlCallV2[Ctx, T]
  case class MutationV2[Ctx, T](field: Field[Ctx, T]) extends GraphqlCallV2[Ctx, T]

  trait FieldLookup {

    def getField[Ctx](schema: Schema[Ctx, _], call: GraphqlCall): Either[GraphqlCallError, Field[Ctx, _]] = {
      val fieldLookup = call match {
        case Query(_)    => schema.query.fieldsByName
        case Mutation(_) => schema.mutation.map(_.fieldsByName).getOrElse(Map())
      }

      fieldLookup.get(call.field) match {
        case None => Left(FieldNotFound(call))
        case Some(fields) =>
          fields.toList match {
            case Nil        => Left(FieldNotFound(call))
            case field :: _ => Right[GraphqlCallError, Field[Ctx, _]](field)
          }
      }
    }

  }

  case class QueryGenerator[C, R](schema: Schema[C, R]) extends FieldLookup {

    def generateQuery(call: GraphqlCall): Either[GraphqlCallError, ast.Document] = {
      getField(schema, call).map { field =>
        val operationType = call match {
          case Query(_)    => ast.OperationType.Query
          case Mutation(_) => ast.OperationType.Mutation
        }
        val argumentsAst = generateArgumentListAst(field.arguments)
        val selectionASt = generateSelectionAst(field.fieldType)

        wrapInDocument(operationType, field, argumentsAst, selectionASt)
      }
    }

    def generateQuery[Ctx, T](call: GraphqlCallV2[Ctx, T]): ast.Document = {
      val (field, operationType) = call match {
        case QueryV2(f)    => (f, ast.OperationType.Query)
        case MutationV2(f) => (f, ast.OperationType.Mutation)
      }
      val argumentsAst = generateArgumentListAst(field.arguments)
      val selectionAst = generateSelectionAst(field.fieldType)

      wrapInDocument(operationType, field, argumentsAst, selectionAst)
    }

    private def wrapInDocument[Ctx, T](
        operationType: ast.OperationType,
        field:         Field[Ctx, T],
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

    private def generateSelectionAst[T](outputType: OutputType[T]): Vector[ast.Selection] = {
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
          Vector(fieldAsts: _*)
        case opt: OptionType[_] =>
          generateSelectionAst(opt.ofType)
        case _ => throw new Exception("WIP Unsupported output type")
      }
    }

    private def generateArgumentListAst[Ctx, T](arguments: List[Argument[_]]): Vector[ast.Argument] = {
      val argumentAsts =
        arguments.map { argument =>
          ast.Argument(argument.name, ast.VariableValue(argument.name))
        }
      Vector(argumentAsts: _*)
    }

  }
}
