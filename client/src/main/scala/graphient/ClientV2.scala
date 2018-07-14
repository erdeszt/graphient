package graphient

import sangria.ast
import sangria.schema._
import graphient.GraphqlCall._

case class ClientV2[C, T](schema: Schema[C, T]) {

  def call(call: GraphqlCall): Either[GraphqlCallError, ast.Document] = {
    val (fieldLookup, operationType) = call match {
      case Query(_)    => (schema.query.fieldsByName, ast.OperationType.Query)
      case Mutation(_) => (schema.mutation.map(_.fieldsByName).getOrElse(Map()), ast.OperationType.Mutation)
    }

    fieldLookup.get(call.field) match {
      case None                           => Left(FieldNotFound(call))
      case Some(fields) if fields.isEmpty => Left(FieldNotFound(call))
      case Some(fields)                   => generateGraphqlAst(operationType, fields.head)
    }
  }

  def generateGraphqlAst[Ctx, R](
      operationType: ast.OperationType,
      field:         Field[Ctx, R]
  ): Either[GraphqlCallError, ast.Document] = {
    val argumentsAst = generateArgumentListAst(field.arguments)
    generateSelectionAst(field).map(wrapInDocument(operationType, field, argumentsAst, _))
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
      case _ => Left(UnsuportedOutputType(outputType))
    }
  }

  private def generateSelectionAst[Ctx, R](field: Field[Ctx, R]): Either[GraphqlCallError, Vector[ast.Selection]] = {
    generateOutputTypeAst(field.fieldType)
  }

  private def generateArgumentListAst[Ctx, R](
      arguments: List[Argument[_]]
  ): Vector[ast.Argument] = {
    Vector(
      arguments.map { argument =>
        ast.Argument(argument.name, ast.VariableValue(argument.name))
      }: _*
    )
  }

}
