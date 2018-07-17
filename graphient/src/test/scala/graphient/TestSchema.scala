package graphient

import sangria.schema._

object TestSchema {

  object Domain {
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
  }

  object Types {
    import Domain._

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

  }

  object Arguments {

    val UserIdArg  = Argument("userId", LongType)
    val NameArg    = Argument("name", StringType)
    val AgeArg     = Argument("age", IntType)
    val HobbiesArg = Argument("hobbies", ListInputType(StringType))

  }

  object Queries {
    import Arguments._
    import Domain._
    import Types._

    val getUser: Field[UserRepo, Unit] =
      Field(
        "getUser",
        OptionType(UserType),
        arguments = UserIdArg :: Nil,
        resolve   = request => request.ctx.getUser(request.args.arg(UserIdArg))
      )

    val schema: ObjectType[UserRepo, Unit] =
      ObjectType(
        "Query",
        fields[UserRepo, Unit](
          getUser
        )
      )

  }

  object Mutations {
    import Arguments._
    import Domain._
    import Types._

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

    val schema: ObjectType[UserRepo, Unit] =
      ObjectType(
        "Mutation",
        fields[UserRepo, Unit](
          createUser
        )
      )
  }

  val schema = Schema(Queries.schema, Some(Mutations.schema))
}
