package graphient

import cats.implicits._
import sangria.ast
import sangria.schema._

import scala.reflect.ClassTag
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
        val (argumentsAst, variableAst) = generateArgumentListAst(field.arguments).unzip
        val selectionASt                = generateSelectionAst(field.fieldType)

        wrapInDocument(operationType, field, argumentsAst, variableAst, selectionASt)
      }
    }

    def generateQuery[Ctx, T](call: GraphqlCallV2[Ctx, T]): ast.Document = {
      val (field, operationType) = call match {
        case QueryV2(f)    => (f, ast.OperationType.Query)
        case MutationV2(f) => (f, ast.OperationType.Mutation)
      }
      val (argumentsAst, variableAst) = generateArgumentListAst(field.arguments).unzip
      val selectionAst                = generateSelectionAst(field.fieldType)

      wrapInDocument(operationType, field, argumentsAst, variableAst, selectionAst)
    }

    private def wrapInDocument[Ctx, T](
        operationType: ast.OperationType,
        field:         Field[Ctx, T],
        arguments:     Vector[ast.Argument],
        variables:     Vector[ast.VariableDefinition],
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
            ),
            variables = variables
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

    // TODO: Fix variable definition generation hack
    // create proper transformation from schema.InputType to ast.Type
    private def generateArgumentListAst[Ctx, T](
        arguments: List[Argument[_]]
    ): Vector[(ast.Argument, ast.VariableDefinition)] = {
      def wrapList(argumentType: InputType[_])(ty: ast.Type): ast.Type = {
        if (argumentType.isList) {
          ast.ListType(wrapNotNull(argumentType)(ty))
        } else {
          ty
        }
      }
      def wrapNotNull(argumentType: InputType[_])(ty: ast.Type): ast.Type = {
        if (argumentType.isOptional) ty else ast.NotNullType(ty)
      }
      arguments.map { argument =>
        val argumentAst = ast.Argument(argument.name, ast.VariableValue(argument.name))
        val namedType   = ast.NamedType(argument.argumentType.namedType.name)
        val variableDefinition = ast.VariableDefinition(
          argument.name,
          wrapNotNull(argument.argumentType)(wrapList(argument.argumentType)(namedType)),
          None
        )

        (argumentAst, variableDefinition)
      }.toVector
    }

  }

  case class VariableGenerator[C, R](schema: Schema[C, R]) extends FieldLookup {

    case class ScalarArgument(argument: Argument[_], value: Any) {

      private def castIfValid[S: ClassTag](value: Any): Option[S] = {
        if (implicitly[ClassTag[S]].runtimeClass.isInstance(value)) {
          Some(value.asInstanceOf[S])
        } else {
          None
        }
      }

      def apply[A: ClassTag](wrapper: A => ast.Value): Either[GraphqlCallError, ast.Value] = {
        castIfValid[A](value).fold {
          Left(InvalidArgumentValue(argument, value)): Either[GraphqlCallError, ast.Value]
        } { validValue =>
          Right(wrapper(validValue)): Either[GraphqlCallError, ast.Value]
        }
      }
    }

    private def argumentTypeValueToAstValue(
        argument:   Argument[_],
        outputType: InputType[_],
        value:      Any
    ): Either[GraphqlCallError, ast.Value] = {
      outputType match {
        case scalar: ScalarType[_] =>
          val arg = ScalarArgument(argument, value)
          scalar.name match {
            // TODO: Why java.lang.Long necessary? erasure because of Any?
            case "Long"   => arg[java.lang.Long](x    => ast.BigIntValue(BigInt(x)))
            case "String" => arg[java.lang.String](x  => ast.StringValue(x))
            case "Int"    => arg[java.lang.Integer](x => ast.IntValue(x))
          }
        case list: ListInputType[_] =>
          value
            .asInstanceOf[Seq[_]]
            .map { item =>
              argumentTypeValueToAstValue(argument, list.ofType, item)
            }
            .toList
            .sequence[Either[GraphqlCallError, ?], ast.Value]
            .map(values => ast.ListValue(values.toVector))
        case _ => throw new Exception("WIP Unsupported argument type")
      }
    }

    def generateVariables(call: GraphqlCall, variableValues: Map[String, Any]): Either[GraphqlCallError, ast.Value] = {
      getField(schema, call).flatMap { field =>
        field.arguments
          .map { argument =>
            variableValues.get(argument.name) match {
              case None => Left(ArgumentNotFound(argument))
              case Some(value) =>
                argumentTypeValueToAstValue(argument, argument.argumentType, value).map { argumentAst =>
                  (argument.name, argumentAst)
                }
            }
          }
          .sequence[Either[GraphqlCallError, ?], (String, ast.Value)]
          .map(fields => ast.ObjectValue(fields: _*))
      }
    }

  }

}
