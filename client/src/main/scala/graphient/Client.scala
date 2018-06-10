package graphient

import sangria.schema._
import sangria.execution._
import sangria.marshalling.circe._
import cats.implicits._
import com.softwaremill.sttp._
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext.Implicits.global

object Client extends App {

  object Test {

    object Domain {
      case class User(
          id:   Long,
          name: String,
          age:  Int
      )

      trait UserRepo {
        def getUser(id: Long): User
      }

      object FakeUserRepo extends UserRepo {
        def getUser(id: Long) = {
          User(id, s"Fake user $id", 20 + id.toInt)
        }
      }
    }

    object Schema {
      import Domain._

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

      val schema = sangria.schema.Schema(QueryType)
    }

    object Server {

      val graphqlService = HttpService[IO] {
        case GET -> Root / "graphql" =>
          Ok(s"Hello, graphql.")
      }

      def apply() = {
        BlazeBuilder[IO]
          .bindHttp(8080, "localhost")
          .mountService(graphqlService, "/")
      }

    }

    def demo(): Unit = {
      println(Client(Schema.schema)(Query("getUser"), Map("userId" -> 1L)))
      val server = Server().start.unsafeRunSync()
      scala.io.StdIn.readLine()
      server.shutdown.unsafeRunSync()
    }
  }

  type ArgMap = Map[String, Any]

  // TODO: Find a better solution or name
  type EitherG[T] = Either[GraphqlCallError, T]

  sealed trait GraphqlCall {
    val field: String
  }

  case class Query(field: String) extends GraphqlCall

  case class Mutation(field: String) extends GraphqlCall

  sealed trait GraphqlCallError
  case class FieldNotFound(graphqlCall:          GraphqlCall) extends GraphqlCallError
  case class ArgumentNotFound[T](argument:       Argument[T]) extends GraphqlCallError
  case class UnsuportedArgumentType[T](argument: Argument[T]) extends GraphqlCallError
  case class UnsuportedOutputType[T](outputType: OutputType[T]) extends GraphqlCallError

  private def generateGraphqlCall[Ctx, R](
      field:     Field[Ctx, R],
      arguments: Client.ArgMap
  ): Either[GraphqlCallError, String] = {
    // TODO: Figure out something better for formatting
    for {
      argumentList <- generateArgumentList(field, arguments)
      fieldList <- generateFields(field)
    } yield s"""${field.name}($argumentList) {
         |\t$fieldList
         |}""".stripMargin
  }

  private def generateArgumentList[Ctx, R](
      field:     Field[Ctx, R],
      arguments: Client.ArgMap
  ): Either[GraphqlCallError, String] = {
    field.arguments
      .map(argument => generateArgument(argument, arguments(argument.name)))
      .sequence[EitherG, String]
      .map(_.mkString(", "))
  }

  private def generateArgument(argument: Argument[_], value: Any): Either[GraphqlCallError, String] = {
    argument.argumentType match {
      case _: ScalarType[Long] => Right(s"$value")
      case _ => Left(UnsuportedArgumentType(argument))
    }
  }

  private def generateFields[Ctx, R](field: Field[Ctx, R]): Either[GraphqlCallError, String] = {
    field.fieldType match {
      case obj: ObjectType[Ctx, _] =>
        obj.fields
          .map(generateField(_))
          .sequence[EitherG, String]
          .map(_.mkString(",\n\t"))
      case _ => Left(UnsuportedOutputType(field.fieldType))
    }
  }

  private def generateField[Ctx, R](value: Field[Ctx, R]): Either[GraphqlCallError, String] = {
    Right(value.name)
  }

  private def queryWrapper(query: String): String = {
    s"""query { 
       |  $query 
       |}""".stripMargin
  }

  private def mutationWrapper(mutation: String): String = {
    s"""mutation {
       |  $mutation
       }""".stripMargin
  }

  def apply[Ctx, R](schema: Schema[Ctx, R])(
      call:                 GraphqlCall,
      argumentValues:       ArgMap
  ): Either[GraphqlCallError, String] = {

    // TODO: Why is the result a Vector???
    val (fields, wrapper) = call match {
      case Query(_)    => (schema.query.fieldsByName, queryWrapper(_))
      case Mutation(_) => (schema.mutation.map(_.fieldsByName).getOrElse(Map()), mutationWrapper(_))
    }

    fields.get(call.field) match {
      case None                           => Left(FieldNotFound(call))
      case Some(fields) if fields.isEmpty => Left(FieldNotFound(call))
      case Some(fields) =>
        val field = fields.head
        val arguments = field.arguments.foldLeft[Either[GraphqlCallError, ArgMap]](Right(Map())) {
          case (e @ Left(_), arg) => e
          case (Right(argvs), arg) =>
            argumentValues.get(arg.name) match {
              case None => Left(ArgumentNotFound(arg))
              // TODO: Check the type of the argument
              case Some(argumentValue) => Right(argvs + (arg.name -> argumentValue))
            }
        }

        arguments.flatMap(generateGraphqlCall(field, _)).map(wrapper(_))
    }
  }

  Test.demo()

}
