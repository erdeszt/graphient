package graphient

import cats.data.OptionT
import cats.effect._
import com.softwaremill.sttp.Response
import graphient.TestSchema.Domain
import graphient.TestSchema.Domain.UserRepo
import io.circe.Json
import org.http4s._
import org.http4s.syntax.KleisliSyntax
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.syntax._
import org.http4s.circe._
import cats.data._
import cats.effect._
import io.circe.syntax._
import cats.implicits._
import org.http4s.headers._
import org.http4s.server.blaze._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeServerBuilder
//import org.http4s.circe._
import io.circe._
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import scala.concurrent.ExecutionContext.Implicits.global

object TestServer extends IOApp with KleisliSyntax {

  case class ErrorResponse(error: String)

  implicit val (errorResponseEncoder, errorResponseDecoders) = {
    (deriveEncoder[ErrorResponse], deriveDecoder[ErrorResponse])
  }

  private def executeQuery(query: Document, op: Option[String], vars: Option[JsonObject]) = {
    val apiService = new UserRepo {
      override def getUser(id: Long): Option[Domain.User] = {
        Some(
          Domain.User(
            id      = 12L,
            name    = "test",
            age     = 123,
            hobbies = List("1231321", "123"),
            address = Domain.Address(
              zip    = 2300,
              city   = "cph",
              street = "et"
            )
          ))

      }

      override def createUser(
          name:    String,
          age:     Option[Int],
          hobbies: List[String],
          address: Domain.Address
      ): Domain.User = {
        Domain.User(
          id      = 12L,
          name    = name,
          age     = age.getOrElse(100),
          hobbies = hobbies,
          address = address
        )
      }
    }
    Executor
      .execute(
        TestSchema.schema,
        query,
        apiService,
        operationName = op,
        variables     = Json.fromJsonObject(vars.getOrElse(JsonObject.empty))
      )
  }

  def run(args: List[String]): IO[ExitCode] = {
    val router = org.http4s.server
      .Router(
        "/" -> HttpRoutes[IO] {
          case GET -> Root => OptionT.liftF(Ok("Front page"))
          case request @ (POST -> Root / "graphql") =>
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
      .as(ExitCode.Success)
  }

}
