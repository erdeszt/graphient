package graphient.specs

import graphient.TestSchema.Domain._
import graphient._
import graphient.model._
import org.scalatest._
import sangria.execution.Executor
import sangria.marshalling.QueryAstInputUnmarshaller

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ExecutionSpec extends FunSpec with Matchers {

  private implicit val queryAstInputUnmarshaller: QueryAstInputUnmarshaller = new QueryAstInputUnmarshaller()
  private val queryGenerator    = new QueryGenerator(TestSchema.schema)
  private val variableGenerator = new VariableGenerator(TestSchema.schema)
  private val defaultUser       = User(1L, "default", 42, List("coding"), Address(1, "city 1", "main street"))
  private val newUserId         = 2L

  object TestUserRepo extends UserRepo {
    override def getUser(id: Long): Option[User] = {
      if (id == defaultUser.id) {
        Some(defaultUser)
      } else {
        None
      }
    }

    override def createUser(name: String, age: Option[Int], hobbies: List[String], address: Address): User = {
      User(newUserId, name, age.getOrElse(100), hobbies, address)
    }
  }

  describe("Local query execution with mock service") {

    it("should execute queries successfully") {
      val query = queryGenerator.generateQuery(Query(TestSchema.Queries.getUser)).right.toOption.get
      val variables = variableGenerator
        .generateVariables(
          TestSchema.Queries.getUser,
          Map("userId" -> 1L)
        )
        .right
        .toOption
        .get
      val result = Await
        .result(
          Executor.execute(
            TestSchema.schema,
            query,
            TestUserRepo,
            variables = variables
          ),
          5 seconds
        )
        .asInstanceOf[Map[String, Any]]
      val data = result.get("data").asInstanceOf[Some[Map[String, Any]]]

      data should not be empty

      val getUser = data.get.get("getUser").asInstanceOf[Some[Map[String, Any]]]

      getUser should not be empty
      getUser.get.get("id") should contain(defaultUser.id)
      getUser.get.get("name") should contain(defaultUser.name)
      getUser.get.get("age") should contain(defaultUser.age)
      getUser.get.get("hobbies") should contain(defaultUser.hobbies)

      val address = getUser.get("address").asInstanceOf[Map[String, Any]]

      address.get("zip") should contain(defaultUser.address.zip)
      address.get("city") should contain(defaultUser.address.city)
      address.get("street") should contain(defaultUser.address.street)
    }

    it("should execute mutations successfully") {
      val name     = "test user"
      val age      = Some(26)
      val hobbies  = List("debugging")
      val zip      = 1
      val city     = "country 1"
      val street   = "main street"
      val mutation = queryGenerator.generateQuery(Mutation(TestSchema.Mutations.createUser)).right.toOption.get
      val variables = variableGenerator
        .generateVariables(
          TestSchema.Mutations.createUser,
          Map(
            "name"    -> name,
            "age"     -> age,
            "hobbies" -> hobbies,
            "address" -> Map(
              "zip"    -> zip,
              "city"   -> city,
              "street" -> street
            )
          )
        )
        .right
        .toOption
        .get
      val result = Await
        .result(
          Executor.execute(
            TestSchema.schema,
            mutation,
            TestUserRepo,
            variables = variables
          ),
          5 seconds
        )
        .asInstanceOf[Map[String, Any]]
      val data = result.get("data").asInstanceOf[Some[Map[String, Any]]]

      data should not be empty

      val createUser = data.get.get("createUser").asInstanceOf[Some[Map[String, Any]]]

      createUser should not be empty
      createUser.get.get("id") should contain(newUserId)
      createUser.get.get("name") should contain(name)
      createUser.get.get("age") should be(age)
      createUser.get.get("hobbies") should contain(hobbies)

      val address = createUser.get("address").asInstanceOf[Map[String, Any]]

      address.get("zip") should contain(zip)
      address.get("city") should contain(city)
      address.get("street") should contain(street)
    }

  }

}
