package graphient.specs

import com.softwaremill.sttp._
import graphient.{GraphientClient, GraphqlCall, GraphqlRequest, Mutation, Query, QueryGenerator, TestSchema}
import graphient.serializer.Encoder
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest._
import org.scalatest.prop.PropertyChecks
import sangria.renderer.QueryRenderer

class ClientSpec extends FunSpec with PropertyChecks with Matchers {

  case class DummyVariables()

  implicit val dummyEncoder: Encoder[GraphqlRequest[DummyVariables]] = new Encoder[GraphqlRequest[DummyVariables]] {
    def encode(requestBody: GraphqlRequest[DummyVariables]): String = {
      s"[request|${requestBody.query}|dummy:encoded]"
    }
  }

  case class Header(key: String, value: String)

  implicit val graphqlCallArb: Arbitrary[GraphqlCall[_, _]] = Arbitrary(
    Gen.oneOf(
      Gen.oneOf(TestSchema.Queries.schema.fields).map(Query(_)):      Gen[GraphqlCall[_, _]],
      Gen.oneOf(TestSchema.Mutations.schema.fields).map(Mutation(_)): Gen[GraphqlCall[_, _]]
    ))
  implicit val headerArb = Arbitrary {
    for {
      key <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString(""))
      value <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString(""))
    } yield Header(key, value)
  }

  val fakeEndpoint   = uri"http://fakehost"
  val client         = new GraphientClient(TestSchema.schema, fakeEndpoint)
  val queryGenerator = new QueryGenerator(TestSchema.schema)
  val defaultHeaders = ("Content-Type", "application/json") :: sttp.headers.toList

  def renderCall(call: GraphqlCall[_, _]): String = {
    queryGenerator.generateQuery(call) match {
      case Left(error)     => throw new Exception(s"Could not render call: ${call} with error: ${error}")
      case Right(document) => QueryRenderer.render(document)
    }
  }

  def assertRight[E, T](value: Either[E, T])(action: T => Unit): Unit = {
    value match {
      case Left(error)       => fail(s"Not right: ${Left(error)}")
      case Right(rightValue) => action(rightValue)
    }
  }

  describe("GraphientClient") {

    describe("createRequest") {

      it("should setup the sttp request correctly") {
        forAll { call: GraphqlCall[_, _] =>
          val renderedQuery = renderCall(call)
          val result        = client.createRequest(call, DummyVariables())

          assertRight(result) { request =>
            request.body shouldBe StringBody(
              dummyEncoder.encode(GraphqlRequest(renderedQuery, DummyVariables())),
              "UTF-8",
              Some("application/json")
            )
            request.method shouldBe Method.POST
            request.uri shouldBe fakeEndpoint
            request.headers should contain theSameElementsAs defaultHeaders
          }
        }
      }

      it("should add extra http headers") {
        forAll { (call: GraphqlCall[_, _], extraHeaders: List[Header]) =>
          val extraHeaderList = extraHeaders.map(h => (h.key, h.value))
          val result          = client.createRequest(call, DummyVariables(), extraHeaderList: _*)

          assertRight(result) { request =>
            request.headers should contain theSameElementsAs (defaultHeaders ++ extraHeaderList.distinct)
          }
        }
      }

    }

  }

}
