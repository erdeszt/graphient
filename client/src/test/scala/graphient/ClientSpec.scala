package graphient

import sangria.ast
import org.scalatest._
import graphient.ClientV2._
import graphient.GraphqlCall._
import graphient.TestSchema.{User, UserRepo}
import sangria.execution.Executor
import sangria.marshalling.QueryAstInputUnmarshaller

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ClientSpec extends FunSpec with Matchers {
  describe("graphient.Client") {
    val testClient = Client(TestSchema())

    it("should generate an ast for a valid request") {
      val queryAst = testClient.call(Query("getUser"), Map("userId" -> 1L))

      queryAst should be('right)
      queryAst.right.toOption.get.definitions should have length 1
      queryAst.toOption.get.definitions.head shouldBe a[ast.OperationDefinition]

      val operation = queryAst.toOption.get.definitions.head.asInstanceOf[ast.OperationDefinition]

      operation.operationType shouldBe a[ast.OperationType.Query.type]
      operation.name shouldBe empty
      operation.variables shouldBe empty
      operation.directives shouldBe empty
      operation.comments shouldBe empty
      operation.trailingComments shouldBe empty
      operation.location shouldBe None
      operation.selections should have length 1
      operation.selections.head shouldBe a[ast.Field]

      val getUserSelection = operation.selections.head.asInstanceOf[ast.Field]

      getUserSelection.alias shouldBe empty
      getUserSelection.name should equal("getUser")
      getUserSelection.directives shouldBe empty
      getUserSelection.comments shouldBe empty
      getUserSelection.trailingComments shouldBe empty
      getUserSelection.location shouldBe empty

      getUserSelection.arguments should have length 1
      getUserSelection.arguments.head.name should equal("userId")
      getUserSelection.arguments.head.value should equal(ast.BigIntValue(BigInt(1L)))

      getUserSelection.selections should have length 4
      getUserSelection.selections(0) shouldBe a[ast.Field]
      getUserSelection.selections(1) shouldBe a[ast.Field]
      getUserSelection.selections(2) shouldBe a[ast.Field]
      getUserSelection.selections(3) shouldBe a[ast.Field]

      val idSelection      = getUserSelection.selections(0).asInstanceOf[ast.Field]
      val nameSelection    = getUserSelection.selections(1).asInstanceOf[ast.Field]
      val ageSelection     = getUserSelection.selections(2).asInstanceOf[ast.Field]
      val hobbiesSelection = getUserSelection.selections(3).asInstanceOf[ast.Field]

      val validateFieldSelection = (fieldSelection: ast.Field, name: String) => {
        fieldSelection.alias shouldBe empty
        fieldSelection.name should equal(name)
        fieldSelection.arguments shouldBe empty
        fieldSelection.directives shouldBe empty
        fieldSelection.selections shouldBe empty
        fieldSelection.comments shouldBe empty
        fieldSelection.trailingComments shouldBe empty
        fieldSelection.location shouldBe empty
      }

      validateFieldSelection(idSelection, "id")
      validateFieldSelection(nameSelection, "name")
      validateFieldSelection(ageSelection, "age")
      validateFieldSelection(hobbiesSelection, "hobbies")
    }

    it("query execution should work") {
      val testUser = User(1L, s"User: 1", 26, List("coding", "debugging"))
      val query    = testClient.call(Query("getUser"), Map("userId" -> 1L)).right.toOption.get
      val testRepo = new UserRepo {
        def getUser(id: Long): Option[User] = {
          Some(testUser)
        }
        def createUser(name: String, age: Int, hobbies: List[String]): User = ???
      }
      val result = Await
        .result(Executor.execute(TestSchema(), query, testRepo), 5 seconds)
        .asInstanceOf[Map[String, Any]]

      val data = result.get("data").asInstanceOf[Some[Map[String, Any]]]

      data should not be empty

      val getUser = data.get.get("getUser").asInstanceOf[Some[Map[String, Any]]]

      getUser should not be empty

      getUser.get.get("id") shouldBe Some(testUser.id)
      getUser.get.get("name") shouldBe Some(testUser.name)
      getUser.get.get("age") shouldBe Some(testUser.age)
      getUser.get.get("hobbies") shouldBe Some(testUser.hobbies)
    }

    it("should handle missing fields") {
      val result = testClient.call(Query("missingField"), Map())

      result should be('left)
      result.left.toOption.get should equal(FieldNotFound(Query("missingField")))
    }

    it("should handle missing arguments") {
      val result = testClient.call(Query("getUser"), Map())

      result should be('left)
      result.left.toOption.get shouldBe a[ArgumentNotFound[_]]
      result.left.toOption.get.asInstanceOf[ArgumentNotFound[_]].argument.name should equal("userId")
    }

    it("should create valid mutations") {
      val mutation = testClient.call(
        Mutation("createUser"),
        Map(
          "name"    -> "test user",
          "age"     -> 21,
          "hobbies" -> List("coding", "debugging")
        )
      )

      mutation should be('right)

      // TODO: Assert ast shape
    }

    it("mutation execution should work") {
      val testUserName    = "test user"
      val testUserAge     = 26
      val testUserHobbies = List("coding", "debugging")
      val mutation = testClient
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
        .result(Executor.execute(TestSchema(), mutation, testRepo), 5 seconds)
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
      val queryGenerator    = QueryGenerator(TestSchema())
      val variableGenerator = VariableGenerator(TestSchema())
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
        TestSchema(),
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
      val queryGenerator    = QueryGenerator(TestSchema())
      val variableGenerator = VariableGenerator(TestSchema())
      val createUserQuery   = queryGenerator.generateQuery(MutationV2(TestSchema.createUser))
      val createUserVariables = variableGenerator
        .generateVariables(
          TestSchema.createUser,
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
        TestSchema(),
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
