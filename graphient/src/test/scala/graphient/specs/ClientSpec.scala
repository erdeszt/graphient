package graphient.specs

import cats.effect.{ExitCase, Fiber, IO, Sync}
import com.softwaremill.sttp._
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

  implicit val contextShift = IO.contextShift(global)
  implicit val timer        = IO.timer(global)
  implicit val backend      = HttpURLConnectionBackend()

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
    Try(sttp.get(uri"http://localhost:8080/status").send().code == 200).getOrElse(false)
  }

  describe("Client - server integration suite") {

    case class EmptyResponse()

    implicit val emptyResponseDecoder = deriveDecoder[EmptyResponse]

    case class GetLongResponse(getLong: Long)

    implicit val getLongDecoder = deriveDecoder[GetLongResponse]

    implicit val idSync = new Sync[Id] {
      def raiseError[A](e:           Throwable): Id[A] = throw e
      def handleErrorWith[A](fa:     Id[A])(f: Throwable => Id[A]): Id[A] = Try(fa).recover { case error => f(error) }.get
      def pure[A](x:                 A): Id[A] = x
      def flatMap[A, B](fa:          Id[A])(f: A => Id[B]): Id[B] = f(fa)
      def suspend[A](thunk:          => Id[A]): Id[A] = thunk
      def bracketCase[A, B](acquire: Id[A])(use: A => Id[B])(release: (A, ExitCase[Throwable]) => Id[Unit]): Id[B] = {
        val resource = acquire
        try {
          val result = use(resource)
          release(resource, ExitCase.Completed)
          result
        } catch {
          case e: Throwable =>
            release(resource, ExitCase.Error(e))
            throw e
        }
      }
      def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] = {
        f(a) match {
          case Left(moreA) => tailRecM[A, B](moreA)(f)
          case Right(b)    => b
        }
      }
    }

    val client = new GraphientClient(TestSchema.schema, uri"http://localhost:8080/graphql")

    it("querying through the client") {
      val response: Id[Response[String]] =
        client.call(Query(TestSchema.Queries.getUser), Params("userId" -> 1L)).send()

      response.code shouldBe 200
    }

    it("querying through the client for no arguments and scalar output") {
      val response: Id[Response[String]] =
        client.call(Query(TestSchema.Queries.getLong), Params()).send()

      response.code shouldBe 200
      response.body shouldBe 'right
      response.body.right.get shouldBe "{\"data\":{\"getLong\":420}}"
    }

    it("decoding to response") {
      val response = client.callAndDecode[Params.T, GetLongResponse](Query(TestSchema.Queries.getLong), Params())

      response shouldBe 'right
      response.right.get.getLong shouldBe 420
    }

    it("decoding error response") {
      val response = client.callAndDecode[Params.T, GetLongResponse](Query(TestSchema.Queries.raiseError), Params())

      response shouldBe 'left

      val errors = response.left.get

      errors should have length 1

      val onlyError = errors.head

      onlyError.message shouldBe "Internal server error"

      response.left.get.head.path should contain theSameElementsInOrderAs List("raiseError")
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
        client.callAndDecode[Params.T, EmptyResponse](Mutation(TestSchema.Mutations.createUser), parameters)

      response shouldBe 'right
    }

  }

}
