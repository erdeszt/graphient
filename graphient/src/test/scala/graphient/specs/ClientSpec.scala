package graphient.specs

import cats.ApplicativeError
import com.softwaremill.sttp._
import cats.effect.{Fiber, IO}
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

    implicit val idApplicativeError = new ApplicativeError[Id, Throwable] {
      def raiseError[A](e:       Throwable): Id[A] = throw e
      def handleErrorWith[A](fa: Id[A])(f: Throwable => Id[A]): Id[A] = Try(fa).recover { case error => f(error) }.get
      def pure[A](x:             A): Id[A] = x
      def ap[A, B](ff:           Id[A => B])(fa: Id[A]): Id[B] = ff(fa)
    }

    val client = new GraphientClient(TestSchema.schema, uri"http://localhost:8080/graphql")

    it("querying through the client") {
      val response = client.call(Query(TestSchema.Queries.getUser), Map[String, Any]("userId" -> 1L))

      response.code shouldBe 200
    }

    it("mutating through the client") {
      val parameters = Map[String, Any](
        "name" -> "test user",
        "age"  -> Some(26),
        "address" -> Map[String, Any](
          "zip"    -> 2300,
          "city"   -> "cph",
          "street" -> "etv"
        ),
        "hobbies" -> List("coding", "debugging")
      )
      val response = client.call(Mutation(TestSchema.Mutations.createUser), parameters)

      response.code shouldBe 200
    }

  }

}
