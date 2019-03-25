package graphient

import cats.data.OptionT
import cats.effect._
import cats.implicits._
import graphient.TestSchema.Domain
import graphient.TestSchema.Domain.UserRepo
import io.circe.{Json, _}
import io.circe.generic.semiauto._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.KleisliSyntax
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import scala.concurrent.ExecutionContext.Implicits.global

object TestServer extends KleisliSyntax {

  case class ErrorResponse(error: String)

  implicit val contextShift = IO.contextShift(global)
  implicit val timer        = IO.timer(global)

  implicit val (errorResponseEncoder, errorResponseDecoders) = {
    (deriveEncoder[ErrorResponse], deriveDecoder[ErrorResponse])
  }

  val fakeUser = Domain.User(
    id      = 12L,
    name    = "test",
    age     = 123,
    hobbies = List("1231321", "123"),
    address = Domain.Address(
      zip    = 2300,
      city   = "cph",
      street = "et"
    )
  )

  object FakeUserRepo extends UserRepo {
    def getUser(id: Long): Option[Domain.User] = { Some(fakeUser) }

    def createUser(
        name:    String,
        age:     Option[Int],
        hobbies: List[String],
        address: Domain.Address
    ): Domain.User = { fakeUser }
  }

  private def executeQuery(query: Document, op: Option[String], vars: Option[JsonObject]) = {
    Executor
      .execute(
        TestSchema.schema,
        query,
        FakeUserRepo,
        operationName = op,
        variables     = Json.fromJsonObject(vars.getOrElse(JsonObject.empty))
      )
  }

  def run: IO[Unit] = {
    val router = org.http4s.server
      .Router(
        "/" -> HttpRoutes[IO] {
          case GET -> Root => OptionT.liftF(Ok("Front page"))
          case request @ POST -> Root / "graphql" =>
            val response = request.as[Json].flatMap {
              json =>
                val query     = root.query.string.getOption(json).get
                val operation = root.operationName.string.getOption(json)
                val variables = root.variables.obj.getOption(json)

                val queryAst = QueryParser.parse(query).getOrElse(throw new Exception("Wrong query"))

                val futureResponse = executeQuery(queryAst, operation, variables).map { responseJson =>
                  Ok(responseJson)
                }

                IO.fromFuture(IO(futureResponse)).flatten

            }
            val or = OptionT.liftF(response)

            or

        }
      )
      .orNotFound

    BlazeServerBuilder[IO]
      .withServiceErrorHandler((_: Request[IO]) => {
        case error: Throwable =>
          IO(println(s"Error: ${error.getMessage}\n${error.getStackTrace.mkString("\n")}")) >>
            InternalServerError(ErrorResponse(error.getMessage).asJson)
      })
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(router)
      .serve
      .compile
      .drain
  }

}
