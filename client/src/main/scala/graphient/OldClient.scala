package graphient

import sangria.ast
import sangria.schema._
import cats.implicits._
import scala.reflect.ClassTag

case class OldClient[C, T](schema: Schema[C, T]) {

  def call(
      call:           GraphqlCall,
      argumentValues: Map[String, Any]
  ): Either[GraphqlCallError, ast.Document] = {
    val (fieldLookup, operationType) = call match {
      case Query(_)    => (schema.query.fieldsByName, ast.OperationType.Query)
      case Mutation(_) => (schema.mutation.map(_.fieldsByName).getOrElse(Map()), ast.OperationType.Mutation)
    }

    fieldLookup.get(call.field) match {
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

  private def generateOutputTypeAst[Ctx, R](
      outputType: OutputType[R]
  ): Either[GraphqlCallError, Vector[ast.Selection]] = {
    outputType match {
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
      case opt: OptionType[_] =>
        generateOutputTypeAst(opt.ofType)
      case _ => throw new Exception("WIP Unsupported output type")
    }
  }

  private def generateSelectionAst[Ctx, R](field: Field[Ctx, R]): Either[GraphqlCallError, Vector[ast.Selection]] = {
    generateOutputTypeAst(field.fieldType)
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

  private def argumentValueToAstValue(argument: Argument[_], value: Any): Either[GraphqlCallError, ast.Value] = {
    argumentTypeValueToAstValue(argument, argument.argumentType, value)
  }

}
