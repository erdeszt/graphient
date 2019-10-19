package graphient.specs

import cats.data.NonEmptyList
import com.softwaremill.sttp._
import com.softwaremill.sttp.testing.SttpBackendStub
import graphient.{GraphientClient, QueryGenerator, TestSchema}
import graphient.model._
import graphient.serializer.{Decoder, Encoder}
import graphient.IdMonadError._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest._
import org.scalatest.prop.PropertyChecks
import sangria.renderer.QueryRenderer

import scala.util.Try

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
            ()
          }
        }
      }

      it("should add extra http headers") {
        forAll { (call: GraphqlCall[_, _], extraHeaders: List[Header]) =>
          val extraHeaderList = extraHeaders.map(h => (h.key, h.value))
          val result          = client.createRequest(call, DummyVariables(), extraHeaderList: _*)

          assertRight(result) { request =>
            request.headers should contain theSameElementsAs (defaultHeaders ++ extraHeaderList.distinct)
            ()
          }
        }
      }

    }

    describe("call") {

      final case class DecoderError() extends Throwable

      type StringDecoder = Decoder[RawGraphqlResponse[String]]

      implicit def nonEmptyListArb[A](implicit arb: Arbitrary[A]): Arbitrary[NonEmptyList[A]] = {
        Arbitrary {
          for {
            head <- arb.arbitrary
            tail <- Gen.listOf(arb.arbitrary)
          } yield NonEmptyList(head, tail)
        }
      }

      implicit val graphqlResponseErrorLocationArb: Arbitrary[GraphqlResponseErrorLocation] = {
        Arbitrary {
          for {
            line <- Gen.chooseNum[Int](0, 100)
            column <- Gen.chooseNum[Int](0, 100)
          } yield GraphqlResponseErrorLocation(line, column)
        }
      }

      implicit val graphqlResponseErrorArb: Arbitrary[GraphqlResponseError] = {
        Arbitrary {
          for {
            message <- Gen.alphaNumStr
            path <- Gen.listOf(Gen.alphaNumStr)
            locations <- Gen.listOf(graphqlResponseErrorLocationArb.arbitrary)
          } yield GraphqlResponseError(message, path, locations)
        }
      }

      def createBackend(call: GraphqlCall[_, _]): SttpBackendStub[Id, Nothing] = {
        SttpBackendStub.synchronous
          .whenRequestMatches { request =>
            // Using matchers for better error messages
            request.body shouldBe StringBody(
              dummyEncoder.encode(GraphqlRequest(renderCall(call), DummyVariables())),
              "UTF-8",
              Some("application/json")
            )
            request.method shouldBe Method.POST
            request.uri shouldBe fakeEndpoint
            request.headers should contain theSameElementsAs defaultHeaders
            // The matchers throw if the request is incorrect so return true if we get here
            true
          }
          // The http response is irrelevant because the decoding result is
          // controlled by the Decoder mock
          .thenRespond("irrelevant")
      }

      it {
        """should return a successful response
          |when the call and the decoding is successful and there are no graphql errors
          |""".stripMargin
      } {
        forAll { (call: GraphqlCall[_, _], decodedResponse: String) =>
          implicit val decoder: StringDecoder = _ => {
            Right(
              RawGraphqlResponse[String](
                data   = Some(Map(call.field.name -> decodedResponse)),
                errors = None
              )
            )
          }
          implicit val backend = createBackend(call)
          val result           = client.call[String](call, DummyVariables())

          result shouldBe decodedResponse
        }
      }

      it("should throw an error when the backend returns an error") {
        forAll { call: GraphqlCall[_, _] =>
          implicit val decoder: StringDecoder = _ => Left(DecoderError())
          implicit val backend = SttpBackendStub.synchronous.whenAnyRequest.thenRespondServerError()

          an[InvalidResponseBody] should be thrownBy {
            client.call[String](call, DummyVariables())
          }
        }
      }

      it("should throw an exception when the decoder returns Left") {
        forAll { call: GraphqlCall[_, _] =>
          implicit val decoder: StringDecoder = _ => Left(DecoderError())
          implicit val backend = createBackend(call)

          a[DecoderError] should be thrownBy {
            client.call[String](call, DummyVariables())
          }
        }
      }

      it("should throw an exception when there's no data and no graphql error") {
        forAll { call: GraphqlCall[_, _] =>
          implicit val decoder: StringDecoder = _ => Right(RawGraphqlResponse(None, None))
          implicit val backend = createBackend(call)

          an[InconsistentResponseNoDataNoError] should be thrownBy {
            client.call[String](call, DummyVariables())
          }
        }
      }

      it("should throw an exception when there's an error field but it's empty") {
        forAll { call: GraphqlCall[_, _] =>
          implicit val decoder: StringDecoder = _ => Right(RawGraphqlResponse(None, Some(Nil)))
          implicit val backend = createBackend(call)

          an[InconsistentResponseEmptyError] should be thrownBy {
            client.call[String](call, DummyVariables())
          }
        }
      }

      it("should throw an exception when there's a graphql error") {
        forAll { (call: GraphqlCall[_, _], errors: NonEmptyList[GraphqlResponseError]) =>
          implicit val decoder: StringDecoder = { _ =>
            Right(RawGraphqlResponse(None, Some(errors.toList)))
          }
          implicit val backend = createBackend(call)

          val result = Try(client.call[String](call, DummyVariables())).toEither

          assert(result.isLeft)
          assert(result.left.get.isInstanceOf[GraphqlResponseError])
          result.left.get shouldBe errors.head
        }
      }

      it("should throw an exception when the field is not present in the response") {
        forAll { (call: GraphqlCall[_, _], response: String) =>
          implicit val decoder: StringDecoder = { _ =>
            Right(RawGraphqlResponse(Some(Map("wrongFieldName" -> response)), None))
          }
          implicit val backend = createBackend(call)

          an[InconsistentResponseNoData] should be thrownBy {
            client.call[String](call, DummyVariables())
          }
        }
      }

    }

  }

}
