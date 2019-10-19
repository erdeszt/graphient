package graphient

import sangria.ast
import sangria.ast.NamedType
import sangria.schema._
import graphient.model._

class QueryGenerator[C, R](schema: Schema[C, R]) extends FieldLookup {

  def generateQuery(call: NamedGraphqlCall): Either[GraphqlCallError, ast.Document] = {
    getField(schema, call).map { field =>
      call match {
        case QueryByName(_)    => generateQueryUnsafe(Query(field))
        case MutationByName(_) => generateQueryUnsafe(Mutation(field))
      }
    }
  }

  def generateQuery[Ctx, T](call: GraphqlCall[Ctx, T]): Either[GraphqlCallError, ast.Document] = {
    getField(schema, call match {
      case Query(field)    => QueryByName(field.name)
      case Mutation(field) => MutationByName(field.name)
    }).map(_ => generateQueryUnsafe(call))
  }

  private def generateQueryUnsafe[Ctx, T](call: GraphqlCall[Ctx, T]): ast.Document = {
    val (field, operationType) = call match {
      case Query(f)    => (f, ast.OperationType.Query)
      case Mutation(f) => (f, ast.OperationType.Mutation)
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
    def unionFields(unionType: UnionType[_]): List[ast.Selection] =
      ast.Field(
        alias      = None,
        name       = "__typename",
        arguments  = Vector(),
        directives = Vector(),
        selections = Vector()
      ) :: unionType.types.map(inlineFragment)
    def inlineFragment(objectType: ObjectType[_, _]): ast.Selection =
      ast.InlineFragment(
        typeCondition = Some(NamedType(name = objectType.name)),
        directives    = Vector(),
        selections    = objectType.fields.map(generateFieldSelectionAst)
      )
    def fieldSelection(field: Field[_, _], selections: Vector[ast.Selection] = Vector.empty): ast.Selection = {
      ast.Field(
        alias      = None,
        name       = field.name,
        arguments  = Vector(),
        directives = Vector(),
        selections = selections
      )
    }
    def generateFieldSelectionAst(field: Field[_, _]): ast.Selection = {
      field.fieldType match {
        case _: ScalarType[_] | _: ScalarAlias[_, _] =>
          fieldSelection(field)
        case _: OptionType[_] =>
          val fields = generateSelectionAst(field.fieldType)
          ast.Field(
            alias      = None,
            name       = field.name,
            arguments  = Vector(),
            directives = Vector(),
            selections = fields
          )
        case _: ListType[_] =>
          val fields = generateSelectionAst(field.fieldType)
          ast.Field(
            alias      = None,
            name       = field.name,
            arguments  = Vector(),
            directives = Vector(),
            selections = fields
          )
        case _: EnumType[_] =>
          ast.Field(
            alias      = None,
            name       = field.name,
            arguments  = Vector(),
            directives = Vector(),
            selections = Vector()
          )
        case obj: ObjectType[_, _] =>
          fieldSelection(field, obj.fields.map(generateFieldSelectionAst))
        case union: UnionType[_] =>
          ast.Field(
            alias      = None,
            name       = field.name,
            arguments  = Vector(),
            directives = Vector(),
            selections = Vector(unionFields(union): _*)
          )
        case _ =>
          throw new Exception(s"WIP Unsupported field type - ${field.name}")
      }
    }

    outputType match {
      case _: ScalarType[_] | _: ScalarAlias[_, _] | _: EnumType[_] =>
        Vector()
      case obj: ObjectType[_, _] =>
        val fieldAsts = obj.fields.map(generateFieldSelectionAst)
        Vector(fieldAsts: _*)
      case list: ListType[_] =>
        generateSelectionAst(list.ofType)
      case opt: OptionType[_] =>
        generateSelectionAst(opt.ofType)
      case union: UnionType[_] =>
        Vector(unionFields(union): _*)
      case _ => throw new Exception(s"WIP Unsupported output type ${outputType.namedType.name}")
    }
  }

  private def generateVariableDefinition(argument: Argument[_]): ast.VariableDefinition = {
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

    val namedType = ast.NamedType(argument.argumentType.namedType.name)

    ast.VariableDefinition(
      argument.name,
      wrapNotNull(argument.argumentType)(wrapList(argument.argumentType)(namedType)),
      None
    )
  }

  private def generateArgumentListAst[Ctx, T](
      arguments: List[Argument[_]]
  ): Vector[(ast.Argument, ast.VariableDefinition)] = {
    arguments.map { argument =>
      val argumentAst        = ast.Argument(argument.name, ast.VariableValue(argument.name))
      val variableDefinition = generateVariableDefinition(argument)

      (argumentAst, variableDefinition)
    }.toVector
  }

}
