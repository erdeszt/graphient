package graphient

import org.scalatest._

class IntegrationTestSuite extends FunSpec with Matchers {

  /*

    val client =
      new GraphienttpClient(TestSchema.schema, uri"http://localhost:8080/graphql")

    val responseQuery =
      client.runQuery(Query(TestSchema.Queries.getUser), Map[String, Any]("userId" -> 1L))

    responseQuery.onComplete {
      case scala.util.Success(r) =>
        println(s"responseQuery: $r")
      case Failure(error) =>
        println(s"errorQuery: $error")
    }

    val createUserCallArguments = Map[String, Any](
      "name"    -> "test user",
      "age"     -> 26,
      "hobbies" -> List("coding", "debugging"),
      "address" -> Map(
        "zip"    -> 1208,
        "city"   -> "cph k",
        "street" -> "ks"
      )
    )
    val responseMutation =
      client.runMutation(Mutation(TestSchema.Mutations.createUser), createUserCallArguments)

    responseMutation.onComplete {
      case scala.util.Success(r) =>
        println(s"responseMutation: $r")
      case Failure(error) =>
        println(s"errorQuery: $error")
    }
   */

  describe("Client - server integration suite") {

    it("should work") {
      true shouldBe true
    }

  }

}
