package graphient

import sangria.macros.derive.deriveInputObjectType
import sangria.marshalling._
import sangria.schema._

object TestSchema {

  object Domain {

    case class ImageId(value: Long)

    case class User(
        id:      Long,
        name:    String,
        age:     Int,
        hobbies: List[String],
        address: Address
    )

    case class Address(
        zip:    Int,
        city:   String,
        street: String
    )

    trait UserRepo {
      def getUser(id:      Long): Option[User]
      def createUser(name: String, age: Option[Int], hobbies: List[String], address: Address): User
    }
  }

  object Types {
    import Domain._

    val ImageId: ScalarAlias[ImageId, Long] =
      ScalarAlias[ImageId, Long](LongType, _.value, value => Right(Domain.ImageId(value)))

    val AddressType = ObjectType(
      "address",
      "Address desc...",
      fields[Unit, Address](
        Field("zip", IntType, resolve       = _.value.zip),
        Field("city", StringType, resolve   = _.value.city),
        Field("street", StringType, resolve = _.value.street)
      )
    )

    val UserType = ObjectType(
      "User",
      "User desc...",
      fields[Unit, User](
        Field("id", LongType, resolve                  = _.value.id),
        Field("name", StringType, resolve              = _.value.name),
        Field("age", OptionType(IntType), resolve      = ctx => Some(ctx.value.age)),
        Field("hobbies", ListType(StringType), resolve = _.value.hobbies),
        Field("address", AddressType, resolve          = _.value.address)
      )
    )

  }

  object Arguments {

    private val AddressInputType = deriveInputObjectType[Domain.Address]()

    implicit val addressFromInput = new FromInput[Domain.Address] {
      val marshaller: ResultMarshaller = CoercedScalaResultMarshaller.default

      def fromResult(node: marshaller.Node): Domain.Address = {
        val rawAddress = node.asInstanceOf[Map[String, Any]]

        Domain.Address(
          rawAddress("zip").asInstanceOf[Int],
          rawAddress("city").asInstanceOf[String],
          rawAddress("street").asInstanceOf[String]
        )
      }
    }

    val UserIdArg  = Argument("userId", LongType)
    val NameArg    = Argument("name", StringType)
    val AgeArg     = Argument("age", OptionInputType(IntType))
    val HobbiesArg = Argument("hobbies", ListInputType(StringType))
    val AddressArg = Argument("address", AddressInputType)

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

    val getLong: Field[UserRepo, Unit] =
      Field(
        "getLong",
        LongType,
        arguments = Nil,
        resolve   = _ => 420L
      )

    val getImageId: Field[UserRepo, Unit] =
      Field(
        "getImageId",
        Types.ImageId,
        arguments = Nil,
        resolve   = _ => Domain.ImageId(123)
      )

    val schema: ObjectType[UserRepo, Unit] =
      ObjectType(
        "Query",
        fields[UserRepo, Unit](
          getUser,
          getLong,
          getImageId
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
        arguments = NameArg :: AgeArg :: HobbiesArg :: AddressArg :: Nil,
        resolve = request => {
          val name    = request.args.arg(NameArg)
          val age     = request.args.arg(AgeArg)
          val hobbies = request.args.arg(HobbiesArg).toList
          val address = request.args.arg(AddressArg)

          request.ctx.createUser(name, age, hobbies, address)
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
