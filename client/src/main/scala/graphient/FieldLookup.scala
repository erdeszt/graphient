package graphient

import sangria.schema._

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
