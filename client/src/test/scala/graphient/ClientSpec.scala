package graphient

import sangria.ast
import org.scalatest._
import graphient.Client
import graphient.GraphqlCall._
import graphient.TestSchema.{User, UserRepo}
import sangria.execution.Executor

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

      getUserSelection.selections should have length 3
      getUserSelection.selections(0) shouldBe a[ast.Field]
      getUserSelection.selections(1) shouldBe a[ast.Field]
      getUserSelection.selections(2) shouldBe a[ast.Field]

      val idSelection   = getUserSelection.selections(0).asInstanceOf[ast.Field]
      val nameSelection = getUserSelection.selections(1).asInstanceOf[ast.Field]
      val ageSelection  = getUserSelection.selections(2).asInstanceOf[ast.Field]

      idSelection.alias shouldBe empty
      idSelection.name should equal("id")
      idSelection.arguments shouldBe empty
      idSelection.directives shouldBe empty
      idSelection.selections shouldBe empty
      idSelection.comments shouldBe empty
      idSelection.trailingComments shouldBe empty
      idSelection.location shouldBe empty

      nameSelection.alias shouldBe empty
      nameSelection.name should equal("name")
      nameSelection.arguments shouldBe empty
      nameSelection.directives shouldBe empty
      nameSelection.selections shouldBe empty
      nameSelection.comments shouldBe empty
      nameSelection.trailingComments shouldBe empty
      nameSelection.location shouldBe empty

      ageSelection.alias shouldBe empty
      ageSelection.name should equal("age")
      ageSelection.arguments shouldBe empty
      ageSelection.directives shouldBe empty
      ageSelection.selections shouldBe empty
      ageSelection.comments shouldBe empty
      ageSelection.trailingComments shouldBe empty
      ageSelection.location shouldBe empty
    }

    it("query execution should work") {
      val query = testClient.call(Query("getUser"), Map("userId" -> 1L)).getOrElse {
        throw new Exception("Invalid query")
      }
      val result = Await
        .result(Executor.execute(TestSchema(), query, new UserRepo {
          def getUser(id: Long): Option[User] = {
            Some(User(id, s"User: $id", 25 + id.toInt))
          }
        }), 5 seconds)
        .asInstanceOf[Map[String, Any]]

      val data = result.get("data").asInstanceOf[Some[Map[String, Any]]]

      data should not be empty

      val getUser = data.get.get("getUser").asInstanceOf[Some[Map[String, Any]]]

      getUser should not be empty

      getUser.get.get("id") shouldBe Some(1L)
      getUser.get.get("name") shouldBe Some("User: 1")
      getUser.get.get("age") shouldBe Some(26)
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
  }
}
