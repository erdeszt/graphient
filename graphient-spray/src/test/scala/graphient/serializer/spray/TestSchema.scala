package graphient.serializer.spray

import sangria.schema._

object TestSchema {

  val getLongQuery: Field[Unit, Unit] = Field(
    "getLong",
    LongType,
    arguments = Nil,
    resolve   = _ => 12L
  )

  val schema = Schema(
    ObjectType(
      "query",
      fields[Unit, Unit](getLongQuery)
    )
  )

}
