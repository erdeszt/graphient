package graphient.serializer

import sttp.client.testing.SttpBackendStub
import graphient.{GraphientClient, QueryGenerator, TestSchema}
import io.circe.generic.semiauto._
import graphient.IdMonadError._
import graphient.serializer.circe._
import org.scalatest.funspec._
import sttp.client._
import graphient.model.{GraphqlRequest, Query}
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import sangria.renderer.QueryRenderer

class CirceEncoderSpec extends AnyFunSpec with Matchers {

  val query = TestSchema.Queries.getLong
  val renderedQuery = QueryRenderer.render {
    new QueryGenerator(TestSchema.schema).generateQuery(Query(query)).right.get
  }

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
        .thenRespond(s"""{"data": { "${query.name}": {} } }""")
      val client = new GraphientClient(TestSchema.schema, uri"http://fakehost")

      client.call[Response](Query(query), Variables()) shouldBe Response()
    }

    ignore("should encode Map[String, Any] correctly") {
      // TOGO: Implement the generator and the test
      ()
    }

  }

}
