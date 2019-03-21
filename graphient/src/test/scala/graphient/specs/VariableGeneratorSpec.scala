package graphient.specs

import graphient._
import graphient.TestSchema.Domain._
import org.scalatest._
import sangria.ast
import sangria.schema.{OptionInputType, OptionType}

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
      val testUserAge     = Some(26)
      val testUserHobbies = List("coding")
      val testAddress     = Address(1, "city 1", "main street")
      val variables = variableGenerator
        .generateVariables(
          TestSchema.Mutations.createUser,
          Map(
            "name"    -> testUserName,
            "age"     -> testUserAge,
            "hobbies" -> testUserHobbies,
            "address" -> Map(
              "zip"    -> testAddress.zip,
              "city"   -> testAddress.city,
              "street" -> testAddress.street
            )
          )
        )
        .right
        .toOption

      variables should not be empty
      variables.get shouldBe a[ast.ObjectValue]
      variables.get.asInstanceOf[ast.ObjectValue].fields should have length 4

      val nameVariable    = variables.get.asInstanceOf[ast.ObjectValue].fields(0)
      val ageVariable     = variables.get.asInstanceOf[ast.ObjectValue].fields(1)
      val hobbiesVariable = variables.get.asInstanceOf[ast.ObjectValue].fields(2)
      val addressVariable = variables.get.asInstanceOf[ast.ObjectValue].fields(3)

      nameVariable.name shouldBe "name"
      nameVariable.value shouldBe a[ast.StringValue]
      nameVariable.value.asInstanceOf[ast.StringValue].value shouldBe testUserName

      ageVariable.name shouldBe "age"
      ageVariable.value shouldBe a[ast.IntValue]
      ageVariable.value.asInstanceOf[ast.IntValue].value shouldBe testUserAge.get

      hobbiesVariable.name shouldBe "hobbies"
      hobbiesVariable.value shouldBe a[ast.ListValue]
      hobbiesVariable.value.asInstanceOf[ast.ListValue].values.toList shouldBe testUserHobbies.map(ast.StringValue(_))

      addressVariable.name shouldBe "address"
      addressVariable.value shouldBe a[ast.ObjectValue]

      val zipVariable    = addressVariable.value.asInstanceOf[ast.ObjectValue].fields(0)
      val cityVariable   = addressVariable.value.asInstanceOf[ast.ObjectValue].fields(1)
      val streetVariable = addressVariable.value.asInstanceOf[ast.ObjectValue].fields(2)

      zipVariable.name shouldBe "zip"
      zipVariable.value shouldBe a[ast.IntValue]
      zipVariable.value.asInstanceOf[ast.IntValue].value shouldBe testAddress.zip

      cityVariable.name shouldBe "city"
      cityVariable.value shouldBe a[ast.StringValue]
      cityVariable.value.asInstanceOf[ast.StringValue].value shouldBe testAddress.city

      streetVariable.name shouldBe "street"
      streetVariable.value shouldBe a[ast.StringValue]
      streetVariable.value.asInstanceOf[ast.StringValue].value shouldBe testAddress.street
    }

    it("should handle missing arguments in queries") {
      val variables = variableGenerator.generateVariables(TestSchema.Queries.getUser, Map[String, Any]())

      variables should be('left)
      variables.left.toOption.get should be(ArgumentNotFound(TestSchema.Arguments.UserIdArg))
    }

    it("should handle missing arguments in mutations") {
      val variables = variableGenerator.generateVariables(
        TestSchema.Mutations.createUser,
        Map[String, Any]("name" -> "test", "age" -> Some(34))
      )

      variables should be('left)
      variables.left.toOption.get should be(ArgumentNotFound(TestSchema.Arguments.HobbiesArg))
    }

    it("should handle missing arguments in mutation fields") {
      val variables = variableGenerator.generateVariables(
        TestSchema.Mutations.createUser,
        Map[String, Any](
          "name"    -> "test",
          "age"     -> Some(34),
          "hobbies" -> List("coding", "debugging"),
          "address" -> Map(
            "zip"  -> 1,
            "city" -> "city 1"
          )
        )
      )

      variables should be('left)
      variables.left.toOption.get should be(ArgumentFieldNotFound(TestSchema.Arguments.AddressArg, "street"))
    }

    it("should handle invalid arguments(Int)") {
      val variables = variableGenerator.generateVariables(
        TestSchema.Mutations.createUser,
        Map[String, Any](
          "name"    -> "valid name",
          "age"     -> Some("invalid age"),
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
          "age"     -> Some(42),
          "hobbies" -> List[String]()
        )
      )

      variables should be('left)
      variables.left.toOption.get should be(InvalidArgumentValue(TestSchema.Arguments.NameArg, 1))
    }

    it("should handle invalid arguments(Object)") {
      val variables = variableGenerator.generateVariables(
        TestSchema.Mutations.createUser,
        Map[String, Any](
          "name"    -> "user 1",
          "age"     -> Some(42),
          "hobbies" -> List[String]("coding"),
          "address" -> "city 1, main street"
        )
      )

      variables should be('left)
      variables.left.toOption.get should be(
        InvalidArgumentValue(TestSchema.Arguments.AddressArg, "city 1, main street"))
    }

  }

}
