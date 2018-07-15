package graphient

import graphient.TestSchema.Domain._
import org.scalatest._
import sangria.ast

class VariableGeneratorSpec extends FunSpec with Matchers {

  val variableGenerator = new VariableGenerator[UserRepo, Unit](TestSchema.schema)

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

    ignore("should handle missing arguments") {
      fail("WIP")
    }

    ignore("should handle invalid arguments") {
      fail("WIP")
    }

  }

}
