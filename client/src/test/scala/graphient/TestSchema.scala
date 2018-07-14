package graphient

import sangria.schema._

object TestSchema {

  case class User(
      id:      Long,
      name:    String,
      age:     Int,
      hobbies: List[String]
  )

  trait UserRepo {
    def getUser(id: Long): Option[User]
  }

  val UserType = ObjectType(
    "User",
    "User desc...",
    fields[Unit, User](
      Field("id", LongType, resolve                  = _.value.id),
      Field("name", StringType, resolve              = _.value.name),
      Field("age", IntType, resolve                  = _.value.age),
      Field("hobbies", ListType(StringType), resolve = _.value.hobbies)
    )
  )

  val UserId = Argument("userId", LongType)

  val QueryType = ObjectType(
    "Query",
    fields[UserRepo, Unit](
      Field(
        "getUser",
        OptionType(UserType),
        arguments = UserId :: Nil,
        resolve   = request => request.ctx.getUser(request.args.arg(UserId))
      )
    )
  )

  def apply() = Schema(QueryType)
}
