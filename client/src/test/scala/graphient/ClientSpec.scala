package graphient

import TestSchema.Domain._
import org.scalatest._
import sangria.ast
import sangria.execution.Executor
import sangria.marshalling.QueryAstInputUnmarshaller

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ClientSpec extends FunSpec with Matchers {
  describe("graphient.Client") {
    val oldtestClient = OldClient(TestSchema.schema)

    it("mutation execution should work") {
      val testUserName    = "test user"
      val testUserAge     = 26
      val testUserHobbies = List("coding", "debugging")
      val mutation = oldtestClient
        .call(
          Mutation("createUser"),
          Map(
            "name"    -> testUserName,
            "age"     -> testUserAge,
            "hobbies" -> testUserHobbies
          )
        )
        .getOrElse {
          throw new Exception("Invalid query")
        }
      val testRepo = new UserRepo {
        def getUser(id:      Long): Option[User] = None
        def createUser(name: String, age: Int, hobbies: List[String]): User = {
          User(1L, name, age, hobbies)
        }
      }
      val result = Await
        .result(Executor.execute(TestSchema.schema, mutation, testRepo), 5 seconds)
        .asInstanceOf[Map[String, Any]]

      val data = result.get("data").asInstanceOf[Some[Map[String, Any]]]

      data should not be empty

      val createUser = data.get.get("createUser").asInstanceOf[Some[Map[String, Any]]]

      createUser should not be empty

      createUser.get.get("id") shouldBe Some(1L)
      createUser.get.get("name") shouldBe Some(testUserName)
      createUser.get.get("age") shouldBe Some(testUserAge)
      createUser.get.get("hobbies") shouldBe Some(testUserHobbies)
    }

    it("V2 example") {
      val queryGenerator    = QueryGenerator(TestSchema.schema)
      val variableGenerator = VariableGenerator(TestSchema.schema)
      val createUserQuery   = queryGenerator.generateQuery(Mutation("createUser")).right.toOption.get
      val createUserVariables = variableGenerator
        .generateVariables(
          Mutation("createUser"),
          Map(
            "name"    -> "test user",
            "age"     -> 26,
            "hobbies" -> List("coding", "debugging")
          )
        )
        .right
        .toOption
        .get
      val testRepo = new UserRepo {
        def getUser(id:      Long): Option[User] = None
        def createUser(name: String, age: Int, hobbies: List[String]): User = {
          User(1L, name, age, hobbies)
        }
      }
      implicit val queryAstInputUnmarshaller: QueryAstInputUnmarshaller = new QueryAstInputUnmarshaller()
      val asyncResult = Executor.execute(
        TestSchema.schema,
        createUserQuery,
        testRepo,
        variables = createUserVariables
      )
      val result = Await.result(asyncResult, 5 seconds).asInstanceOf[Map[String, Any]]

      val data = result.get("data").asInstanceOf[Some[Map[String, Any]]]

      data should not be empty

      val createUser = data.get.get("createUser").asInstanceOf[Some[Map[String, Any]]]

      createUser should not be empty

      createUser.get.get("id") shouldBe Some(1L)
      createUser.get.get("name") shouldBe Some("test user")
      createUser.get.get("age") shouldBe Some(26)
      createUser.get.get("hobbies") shouldBe Some(List("coding", "debugging"))

    }

    it("V2 with V2 call example") {
      val queryGenerator    = QueryGenerator(TestSchema.schema)
      val variableGenerator = VariableGenerator(TestSchema.schema)
      val createUserQuery   = queryGenerator.generateQuery(MutationV2(TestSchema.Mutations.createUser))
      val createUserVariables = variableGenerator
        .generateVariables(
          TestSchema.Mutations.createUser,
          Map(
            "name"    -> "test user",
            "age"     -> 26,
            "hobbies" -> List("coding", "debugging")
          )
        )
        .right
        .toOption
        .get
      val testRepo = new UserRepo {
        def getUser(id:      Long): Option[User] = None
        def createUser(name: String, age: Int, hobbies: List[String]): User = {
          User(1L, name, age, hobbies)
        }
      }
      implicit val queryAstInputUnmarshaller: QueryAstInputUnmarshaller = new QueryAstInputUnmarshaller()
      val asyncResult = Executor.execute(
        TestSchema.schema,
        createUserQuery,
        testRepo,
        variables = createUserVariables
      )
      val result = Await.result(asyncResult, 5 seconds).asInstanceOf[Map[String, Any]]

      val data = result.get("data").asInstanceOf[Some[Map[String, Any]]]

      data should not be empty

      val createUser = data.get.get("createUser").asInstanceOf[Some[Map[String, Any]]]

      createUser should not be empty

      createUser.get.get("id") shouldBe Some(1L)
      createUser.get.get("name") shouldBe Some("test user")
      createUser.get.get("age") shouldBe Some(26)
      createUser.get.get("hobbies") shouldBe Some(List("coding", "debugging"))

    }
  }
}
