package graphient.serializer

import com.softwaremill.sttp.testing.SttpBackendStub
import graphient.{GraphientClient, QueryGenerator}
import io.circe.generic.semiauto._
import graphient.IdMonadError._
import graphient.serializer.circe._
import org.scalatest._
import sangria.schema._
import com.softwaremill.sttp._
import graphient.model.{GraphqlRequest, Query}
import io.circe.syntax._
import sangria.renderer.QueryRenderer

class CirceEncoderSpec extends FunSpec with Matchers {

  val query: Field[Unit, Unit] = Field("test", StringType, resolve = _ => "response")
  val schema        = Schema(ObjectType[Unit, Unit]("Query", fields[Unit, Unit](query)))
  val renderedQuery = QueryRenderer.render(new QueryGenerator(schema).generateQuery(Query(query)).right.get)

  case class Variables()
  case class Response()

  implicit val variablesEncoder = deriveEncoder[Variables]
  implicit val responseDecoder  = deriveDecoder[Response]

  describe("Circe codecs") {

    it("should encode and decode correctly") {
      implicit val backend = SttpBackendStub.synchronous
        .whenRequestMatches { request =>
          request.body.asInstanceOf[StringBody].s shouldBe GraphqlRequest(renderedQuery, Variables()).asJson.noSpaces
          true
        }
        .thenRespond("""{"data": { "test": {} } }""")
      val client = new GraphientClient(schema, uri"http://fakehost")

      client.call[Response](Query(query), Variables()) shouldBe Response()
    }

  }

}
