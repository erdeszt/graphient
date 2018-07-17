package graphient

import cats.implicits._
import sangria.ast
import sangria.schema._

import scala.reflect.ClassTag

class VariableGenerator[C, R](schema: Schema[C, R]) extends FieldLookup {

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
          case "Int"    => arg[java.lang.Integer](x => ast.IntValue(x))
          case "Long"   => arg[java.lang.Long](x    => ast.BigIntValue(BigInt(x)))
          case "String" => arg[java.lang.String](x  => ast.StringValue(x))
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

  def generateVariables[Ctx, T](
      field:          Field[Ctx, T],
      variableValues: Map[String, Any]
  ): Either[GraphqlCallError, ast.Value] = {
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

  def generateVariables(
      call:           NamedGraphqlCall,
      variableValues: Map[String, Any]
  ): Either[GraphqlCallError, ast.Value] = {
    getField(schema, call).flatMap(generateVariables(_, variableValues))
  }

  def generateVariables[Ctx, T](
      call:           GraphqlCall[Ctx, T],
      variableValues: Map[String, Any]
  ): Either[GraphqlCallError, ast.Value] = {
    generateVariables(call.field, variableValues)
  }

}
