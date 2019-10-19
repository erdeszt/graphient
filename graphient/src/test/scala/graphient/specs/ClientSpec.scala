package graphient.specs

import com.softwaremill.sttp._
import com.softwaremill.sttp.testing.SttpBackendStub
import graphient.{GraphientClient, QueryGenerator, TestSchema}
import graphient.model._
import graphient.serializer.{Decoder, Encoder}
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

      implicit val idMonadError = new cats.MonadError[Id, Throwable] {
        def raiseError[A](e:       Throwable): Id[A] = throw e
        def handleErrorWith[A](fa: Id[A])(f: Throwable => Id[A]): Id[A] = {
          try (fa)
          catch { case ex: Throwable => f(ex) }
        }
        def pure[A](x:        A): Id[A] = x
        def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
        def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] = {
          f(a) match {
            case Left(a)  => tailRecM[A, B](a)(f)
            case Right(b) => b
          }
        }
      }

      def createDecoder(result: Either[Throwable, RawGraphqlResponse[String]]): Decoder[RawGraphqlResponse[String]] = {
        responseBody: String =>
          result
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
          |if the call and the decoding is successful and there are no graphql errors
          |""".stripMargin
      } {
        forAll { call: GraphqlCall[_, _] =>
          val successMessage = "success"
          implicit val decoder = createDecoder {
            Right(
              RawGraphqlResponse[String](
                data   = Some(Map(call.field.name -> successMessage)),
                errors = None
              )
            )
          }
          implicit val backend = createBackend(call)
          val result           = client.call[Id, DummyVariables, String](call, DummyVariables())

          result shouldBe successMessage
        }
      }
    }

  }

}
