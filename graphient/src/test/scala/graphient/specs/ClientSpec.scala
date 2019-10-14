package graphient.specs

import cats.effect.{Fiber, IO}
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import graphient._
import graphient.Implicits._
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class ClientSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  case class GetUserPayload(userId: Long)

  object GetUserPayload {
    implicit val gupEncoder: Encoder[GetUserPayload] = deriveEncoder[GetUserPayload]
  }

  implicit val contextShift   = IO.contextShift(global)
  implicit val timer          = IO.timer(global)
  implicit val backend        = AsyncHttpClientCatsBackend[IO]()
  implicit val addressDecoder = deriveDecoder[TestSchema.Domain.Address]
  implicit val userDecoder    = deriveDecoder[TestSchema.Domain.User]

  var serverThread = None: Option[Fiber[IO, Unit]]

  override def beforeAll() = {
    serverThread = Some(TestServer.run.start.unsafeRunSync)

    while (!isServerUp()) {
      Thread.sleep(1000)
    }
  }

  override def afterAll() = {
    serverThread.foreach(_.cancel.unsafeRunSync)
  }

  private def isServerUp(): Boolean = {
    Try(sttp.get(uri"http://localhost:8080/status").send().unsafeRunSync().code == 200).getOrElse(false)
  }

  describe("Client - server integration suite") {

    case class EmptyResponse()

    implicit val emptyResponseDecoder = deriveDecoder[EmptyResponse]

    val client = new GraphientClient(TestSchema.schema, uri"http://localhost:8080/graphql")

    it("querying through the client") {
      val expectedToken = "Bearer token"
      val request: Either[GraphqlCallError, Request[String, Nothing]] = client
        .createRequest(Query(TestSchema.Queries.getUser), Params("userId" -> 1L), "Authorization" -> expectedToken)

      request.isRight shouldBe true
      request.right.get.headers.exists { case (k, v) => k.equals("Authorization") && v.equals(expectedToken) } shouldBe true

      val response: IO[Response[String]] = request.right.get.send()

      response.unsafeRunSync().code shouldBe 200
    }

    it("querying through the client for no arguments, no headers and scalar output") {
      val request = client.createRequest(Query(TestSchema.Queries.getLong), Params())

      request.right.get.headers.map(_._1).toSet shouldBe Set("Accept-Encoding", "Content-Type")

      val response: Response[String] = request.right.get.send().unsafeRunSync()

      response.code shouldBe 200
      response.body shouldBe 'right
      response.body.right.get shouldBe "{\"data\":{\"getLong\":420}}"
    }

    it("decoding to response") {
      val response =
        client
          .call[IO, Params.T, Long](Query(TestSchema.Queries.getLong), Params())
          .unsafeRunSync()

      response shouldBe 420
    }

    it("decoding to object response") {
      val response =
        client
          .call[IO, Params.T, TestSchema.Domain.User](Query(TestSchema.Queries.getUser), Params("userId" -> 1L))
          .unsafeRunSync()

      response.id shouldBe 12L
      response.name shouldBe "test"
    }

    it("decoding error response") {
      val response =
        client
          .call[IO, Params.T, Long](Query(TestSchema.Queries.raiseError), Params())
          .map(Right(_): Either[Throwable, Long])
          .handleErrorWith(error => IO.pure(Left(error)))
          .unsafeRunSync()

      response shouldBe 'left

      val error = response.left.get

      error.asInstanceOf[GraphqlResponseError].message shouldBe "Internal server error"
    }

    // TODO: Check for errors
    it("mutating through the client") {
      val parameters = Params(
        "name" -> "test user",
        "age"  -> Some(26),
        "address" -> Params(
          "zip"    -> 2300,
          "city"   -> "cph",
          "street" -> "etv"
        ),
        "hobbies" -> List("coding", "debugging")
      )
      val response =
        client
          .call[IO, Params.T, EmptyResponse](Mutation(TestSchema.Mutations.createUser), parameters)
          .unsafeRunSync()

      response shouldBe EmptyResponse()
    }

  }

}
