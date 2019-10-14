package graphient.serializer.spray

import com.softwaremill.sttp._
import cats.effect.IO
import org.scalatest._
import spray.json._
import graphient.serializer.Spray._
import graphient.{GraphientClient, Params, Query}
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import scala.concurrent.ExecutionContext.Implicits.global

class SprayEncoderSpec extends FunSpec with Matchers {

  implicit val contextShift = IO.contextShift(global)
  implicit val timer        = IO.timer(global)
  implicit val backend      = AsyncHttpClientCatsBackend[IO]()
  val client                = new GraphientClient(TestSchema.schema, uri"http://localhost:8080/graphql")

  case class EmptyRequest()

  object EmptyRequest extends DefaultJsonProtocol {
    implicit val sprayFormat = jsonFormat0(() => EmptyRequest())
  }

  describe("Spray json encoder and decoder tests") {

    it("should compile with spray encoder and decoder") {
      client
        .call[IO, EmptyRequest, Long](Query(TestSchema.getLongQuery), EmptyRequest())
      ()
    }

    it("should compile with Params as input") {
      client
        .call[IO, Params.T, Long](Query(TestSchema.getLongQuery), Map[String, Any]())
      ()
    }

  }

}
