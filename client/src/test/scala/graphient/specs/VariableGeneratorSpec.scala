package graphient.specs

import graphient._
import graphient.TestSchema.Domain._
import org.scalatest._
import sangria.ast

class VariableGeneratorSpec extends FunSpec with Matchers {

  private val variableGenerator = new VariableGenerator[UserRepo, Unit](TestSchema.schema)

  describe("VariableGenerator") {

    it("should generate valid variable ast for queries") {
      val variables = variableGenerator
        .generateVariables(
          TestSchema.Queries.getUser,
          Map("userId" -> 1L)
        )
        .right
        .toOption

      variables should not be empty
      variables.get shouldBe a[ast.ObjectValue]
      variables.get.asInstanceOf[ast.ObjectValue].fields should have length 1

      val userIdVariable = variables.get.asInstanceOf[ast.ObjectValue].fields.head

      userIdVariable.name shouldBe "userId"
      userIdVariable.value shouldBe a[ast.BigIntValue]
      userIdVariable.value.asInstanceOf[ast.BigIntValue].value shouldBe BigInt(1L)
    }

    it("should generate valid variable ast for mutations") {
      val testUserName    = "test user"
      val testUserAge     = 26
      val testUserHobbies = List("coding")
      val variables = variableGenerator
        .generateVariables(
          TestSchema.Mutations.createUser,
          Map(
            "name"    -> testUserName,
            "age"     -> testUserAge,
            "hobbies" -> testUserHobbies
          )
        )
        .right
        .toOption

      variables should not be empty
      variables.get shouldBe a[ast.ObjectValue]
      variables.get.asInstanceOf[ast.ObjectValue].fields should have length 3

      val nameVariable    = variables.get.asInstanceOf[ast.ObjectValue].fields(0)
      val ageVariable     = variables.get.asInstanceOf[ast.ObjectValue].fields(1)
      val hobbiesVariable = variables.get.asInstanceOf[ast.ObjectValue].fields(2)

      nameVariable.name shouldBe "name"
      nameVariable.value shouldBe a[ast.StringValue]
      nameVariable.value.asInstanceOf[ast.StringValue].value shouldBe testUserName

      ageVariable.name shouldBe "age"
      ageVariable.value shouldBe a[ast.IntValue]
      ageVariable.value.asInstanceOf[ast.IntValue].value shouldBe testUserAge

      hobbiesVariable.name shouldBe "hobbies"
      hobbiesVariable.value shouldBe a[ast.ListValue]
      hobbiesVariable.value.asInstanceOf[ast.ListValue].values.toList shouldBe testUserHobbies.map(ast.StringValue(_))
    }

    it("should handle missing arguments in queries") {
      val variables = variableGenerator.generateVariables(TestSchema.Queries.getUser, Map[String, Any]())

      variables should be('left)
      variables.left.toOption.get should be(ArgumentNotFound(TestSchema.Arguments.UserIdArg))
    }

    it("should handle missing arguments in mutations") {
      val variables = variableGenerator.generateVariables(
        TestSchema.Mutations.createUser,
        Map[String, Any]("name" -> "test", "age" -> 34)
      )

      variables should be('left)
      variables.left.toOption.get should be(ArgumentNotFound(TestSchema.Arguments.HobbiesArg))
    }

    it("should handle invalid arguments(Int)") {
      val variables = variableGenerator.generateVariables(
        TestSchema.Mutations.createUser,
        Map[String, Any](
          "name"    -> "valid name",
          "age"     -> "invalid age",
          "hobbies" -> List[String]()
        )
      )

      variables should be('left)
      variables.left.toOption.get should be(InvalidArgumentValue(TestSchema.Arguments.AgeArg, "invalid age"))
    }

    it("should handle invalid arguments(Long)") {
      val variables = variableGenerator.generateVariables(
        TestSchema.Queries.getUser,
        Map[String, Any]("userId" -> "invalidId")
      )

      variables should be('left)
      variables.left.toOption.get should be(InvalidArgumentValue(TestSchema.Arguments.UserIdArg, "invalidId"))
    }

    it("should handle invalid arguments(String)") {
      val variables = variableGenerator.generateVariables(
        TestSchema.Mutations.createUser,
        Map[String, Any](
          "name"    -> 1,
          "age"     -> 42,
          "hobbies" -> List[String]()
        )
      )

      variables should be('left)
      variables.left.toOption.get should be(InvalidArgumentValue(TestSchema.Arguments.NameArg, 1))
    }

  }

}
