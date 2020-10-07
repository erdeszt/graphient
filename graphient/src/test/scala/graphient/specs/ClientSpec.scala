package graphient.specs

import cats.Id
import cats.data.NonEmptyList
import sttp.client._
import sttp.client.testing.SttpBackendStub
import graphient.{Generators, GraphientClient, QueryGenerator, TestSchema, VariableGenerator}
import graphient.model._
import graphient.serializer._
import graphient.IdMonadError._
import graphient.TestSchema.Domain
import graphient.TestSchema.Domain.UserRepo
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sangria.execution.Executor
import sangria.renderer.QueryRenderer
import sttp.model.{HeaderNames, MediaType, Method}

import scala.util.Try

class ClientSpec extends AnyFunSpec with ScalaCheckPropertyChecks with Matchers with Generators {

  case class DummyVariables()

  implicit val dummyEncoder: Encoder[GraphqlRequest[DummyVariables]] = new Encoder[GraphqlRequest[DummyVariables]] {
    def encode(requestBody: GraphqlRequest[DummyVariables]): String = {
      s"[request|${requestBody.query}|dummy:encoded]"
    }
  }

  val fakeEndpoint   = uri"http://fakehost"
  val client         = new GraphientClient(TestSchema.schema, fakeEndpoint)
  val queryGenerator = new QueryGenerator(TestSchema.schema)
  val defaultHeaders = {
    List(
      sttp.model.Header(HeaderNames.ContentType, "application/json"),
      sttp.model.Header(HeaderNames.AcceptEncoding, "gzip, deflate")
    )
  }

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
              Some(MediaType.ApplicationJson)
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
          val extraHeaderList  = extraHeaders.map(h => (h.key, h.value))
          val result           = client.createRequest(call, DummyVariables(), extraHeaderList: _*)
          val extraSttpHeaders = extraHeaderList.map { case (k, v) => sttp.model.Header(k, v) }

          assertRight(result) { request =>
            request.headers should contain theSameElementsAs (defaultHeaders ++ extraSttpHeaders.distinct)
            ()
          }
        }
      }

    }

    describe("call") {

      final case class DecoderError() extends Throwable

      type StringDecoder = Decoder[RawGraphqlResponse[String]]

      def createBackend(call: GraphqlCall[_, _]): SttpBackendStub[Id, Nothing] = {
        SttpBackendStub.synchronous
          .whenRequestMatches { request =>
            // Using matchers for better error messages
            request.body shouldBe StringBody(
              dummyEncoder.encode(GraphqlRequest(renderCall(call), DummyVariables())),
              "UTF-8",
              Some(MediaType.ApplicationJson)
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

      describe("integration with the Executor") {
        import scala.concurrent.Await
        import scala.concurrent.duration._
        import scala.concurrent.ExecutionContext.Implicits.global
        import sangria.ast
        import sangria.marshalling.QueryAstInputUnmarshaller

        implicit val queryAstInputUnmarshaller: QueryAstInputUnmarshaller = new QueryAstInputUnmarshaller()

        def generateQuery(call: GraphqlCall[_, _]): ast.Document = {
          new QueryGenerator(TestSchema.schema).generateQuery(call).right.get
        }

        def generateVariables(call: GraphqlCall[_, _], values: Map[String, Long]): ast.Value = {
          new VariableGenerator(TestSchema.schema).generateVariables(call, values).right.get
        }

        object TestUserRepo extends UserRepo {
          val user = Domain.User(1L, "user1", 1, List(), Domain.Address(1, "city", "street"))
          def getUser(id: Long): Option[Domain.User] = Some(user)
          def createUser(
              name:    String,
              age:     Option[Int],
              hobbies: List[String],
              address: Domain.Address
          ): Domain.User = user
        }

        it("should generate a response with the correct field name") {
          val call = Query(TestSchema.Queries.getUser)
          val result = Await
            .result(
              Executor.execute(
                TestSchema.schema,
                generateQuery(call),
                TestUserRepo,
                variables = generateVariables(call, Map("userId" -> 1L))
              ),
              5 seconds
            )
            .asInstanceOf[Map[String, Any]]

          val data = result("data").asInstanceOf[Map[String, Any]]

          assert(data.contains(call.field.name))
        }

      }

    }

  }

}
