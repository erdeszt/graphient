package graphient

import sangria.schema._

object TestSchema {

  case class User(
      id:   Long,
      name: String,
      age:  Int
  )

  trait UserRepo {
    def getUser(id: Long): User
  }

  val UserType = ObjectType(
    "User",
    "User desc...",
    fields[Unit, User](
      Field("id", LongType, resolve     = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("age", IntType, resolve     = _.value.age)
    )
  )

  val UserId = Argument("userId", LongType)

  val QueryType = ObjectType(
    "Query",
    fields[UserRepo, Unit](
      Field(
        "getUser",
        UserType,
        arguments = UserId :: Nil,
        resolve   = request => request.ctx.getUser(request.args.arg(UserId))
      )
    )
  )

  def apply() = Schema(QueryType)
}
