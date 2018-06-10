package graphient

import sangria.ast
import sangria.schema._
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser._
import cats.implicits._
import com.softwaremill.sttp._
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

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

      def apply() = sangria.schema.Schema(QueryType)
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
      Client.generateGraphqlAst(Schema().query.fieldsByName("getUser").head, Map("userId" -> 1L)) match {
        case Left(error) => println(s"Failed to generate graphql ast: $error")
        case Right(ast) =>
          val result = Await.result(Executor.execute(Schema(), ast, Domain.FakeUserRepo), 5 seconds)

          print(s"Result: $result")
      }
    }
  }

  type ArgMap = Map[String, Any]

  // TODO: Find a better solution or name
  type EitherG[T] = Either[GraphqlCallError, T]

  sealed trait GraphqlCall {
    val field: String
  }
  case class Query(field:    String) extends GraphqlCall
  case class Mutation(field: String) extends GraphqlCall

  sealed trait GraphqlCallError
  case class FieldNotFound(graphqlCall:          GraphqlCall) extends GraphqlCallError
  case class ArgumentNotFound[T](argument:       Argument[T]) extends GraphqlCallError
  case class UnsuportedArgumentType[T](argument: Argument[T]) extends GraphqlCallError
  case class UnsuportedOutputType[T](outputType: OutputType[T]) extends GraphqlCallError

  def generateGraphqlAst[Ctx, R](
      field:     Field[Ctx, R],
      arguments: ArgMap
  ): Either[GraphqlCallError, ast.Document] = {
    for {
      argumentsAst <- generateArgumentListAst(field.arguments, arguments)
      selectionsAst <- generateSelectionAst(field)
    } yield wrapInDocument(field, argumentsAst, selectionsAst)
  }

  private def wrapInDocument[Ctx, R](
      field:      Field[Ctx, R],
      arguments:  Vector[ast.Argument],
      selections: Vector[ast.Selection]
  ): ast.Document = {
    ast.Document(
      Vector(
        ast.OperationDefinition(
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

  private def generateSelectionAst[Ctx, R](field: Field[Ctx, R]): Either[GraphqlCallError, Vector[ast.Selection]] = {
    field.fieldType match {
      case obj: ObjectType[Ctx, _] =>
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
      case _ => Left(UnsuportedOutputType(field.fieldType))
    }
  }

  private def generateArgumentListAst[Ctx, R](
      arguments:      List[Argument[_]],
      argumentValues: ArgMap
  ): Either[GraphqlCallError, Vector[ast.Argument]] = {
    arguments
      .map { argument =>
        argumentValues.get(argument.name) match {
          case None => Left(ArgumentNotFound(argument))
          case Some(argumentValue) =>
            argumentValueToAstValue(argument, argumentValue).map(ast.Argument(argument.name, _))
        }
      }
      .sequence[EitherG, ast.Argument]
      .map(argumentList => Vector(argumentList: _*))
  }

  private def argumentValueToAstValue(argument: Argument[_], value: Any): Either[GraphqlCallError, ast.Value] = {
    argument.argumentType match {
      case longValue: ScalarType[Long] => Right(ast.BigIntValue(BigInt(value.asInstanceOf[Long])))
      case _ => Left(UnsuportedArgumentType(argument))
    }
  }

  def apply[Ctx, R](schema: Schema[Ctx, R])(
      call:                 GraphqlCall,
      argumentValues:       ArgMap
  ): Either[GraphqlCallError, ast.Document] = {

    // TODO: Why is the result a Vector???
    val fields = call match {
      case Query(_)    => schema.query.fieldsByName
      case Mutation(_) => schema.mutation.map(_.fieldsByName).getOrElse(Map())
    }

    fields.get(call.field) match {
      case None                           => Left(FieldNotFound(call))
      case Some(fields) if fields.isEmpty => Left(FieldNotFound(call))
      case Some(fields)                   => generateGraphqlAst(fields.head, argumentValues)
    }
  }

  Test.demo()

}
