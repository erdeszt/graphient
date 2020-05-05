package graphient.serializer

import sttp.client.testing.SttpBackendStub
import graphient.{GraphientClient, QueryGenerator, TestSchema}
import graphient.IdMonadError._
import graphient.serializer.spray._
import sttp.client._
import graphient.model.{GraphqlRequest, Query}
import _root_.spray.json._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import sangria.renderer.QueryRenderer

class SprayEncoderSpec extends AnyFunSpec with Matchers with DefaultJsonProtocol {

  val query         = TestSchema.Queries.getLong
  val renderedQuery = QueryRenderer.render(new QueryGenerator(TestSchema.schema).generateQuery(Query(query)).right.get)

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
        .thenRespond(s"""{"data": { "${query.name}": {} } }""")
      val client = new GraphientClient(TestSchema.schema, uri"http://fakehost")

      client.call[Response](Query(query), Variables()) shouldBe Response()
    }

    ignore("should encode Map[String, Any] correctly") {
      // TOGO: Implement the encoder, the generator and the test
      ()
    }

  }

}
