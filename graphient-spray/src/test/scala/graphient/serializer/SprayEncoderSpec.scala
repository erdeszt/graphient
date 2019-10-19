package graphient.serializer

import org.scalatest._
import com.softwaremill.sttp.testing.SttpBackendStub
import graphient.{GraphientClient, QueryGenerator}
import graphient.IdMonadError._
import graphient.serializer.spray._
import org.scalatest._
import sangria.schema._
import com.softwaremill.sttp._
import graphient.model.{GraphqlRequest, Query}
import _root_.spray.json._
import sangria.renderer.QueryRenderer

class CirceEncoderSpec extends FunSpec with Matchers with DefaultJsonProtocol {

  val query: Field[Unit, Unit] = Field("test", StringType, resolve = _ => "response")
  val schema        = Schema(ObjectType[Unit, Unit]("Query", fields[Unit, Unit](query)))
  val renderedQuery = QueryRenderer.render(new QueryGenerator(schema).generateQuery(Query(query)).right.get)

  case class Variables()
  case class Response()

  implicit val variableFormat = jsonFormat0(() => Variables())
  implicit val responseFormat = jsonFormat0(() => Response())

  describe("Circe codecs") {

    it("should compile") {
      implicit val backend = SttpBackendStub.synchronous
        .whenRequestMatches { request =>
          request.body
            .asInstanceOf[StringBody]
            .s shouldBe GraphqlRequest(renderedQuery, Variables()).toJson.compactPrint
          true
        }
        .thenRespond("""{"data": { "test": {} } }""")
      val client = new GraphientClient(schema, uri"http://fakehost")

      client.call[Response](Query(query), Variables()) shouldBe Response()
    }

  }

}
