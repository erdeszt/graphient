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
    def getUser(id:      Long): Option[User]
    def createUser(name: String, age: Int, hobbies: List[String]): User
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

  val UserIdArg  = Argument("userId", LongType)
  val NameArg    = Argument("name", StringType)
  val AgeArg     = Argument("age", IntType)
  val HobbiesArg = Argument("hobbies", ListInputType(StringType))

  val QueryType = ObjectType(
    "Query",
    fields[UserRepo, Unit](
      Field(
        "getUser",
        OptionType(UserType),
        arguments = UserIdArg :: Nil,
        resolve   = request => request.ctx.getUser(request.args.arg(UserIdArg))
      )
    )
  )

  val createUser: Field[UserRepo, Unit] =
    Field(
      "createUser",
      UserType,
      arguments = NameArg :: AgeArg :: HobbiesArg :: Nil,
      resolve = request => {
        val name    = request.args.arg(NameArg)
        val age     = request.args.arg(AgeArg)
        val hobbies = request.args.arg(HobbiesArg).toList

        request.ctx.createUser(name, age, hobbies)
      }
    )

  val MutationType = ObjectType(
    "Mutation",
    fields[UserRepo, Unit](
      createUser
    )
  )

  def apply() = Schema(QueryType, Some(MutationType))
}
