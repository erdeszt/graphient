import graphient._
import graphient.TestSchema.Domain._
import sangria.ast
import sangria.renderer._
import sangria.execution._
import sangria.marshalling.QueryAstInputUnmarshaller

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

implicit val queryAstInputUnmarshaller: QueryAstInputUnmarshaller = new QueryAstInputUnmarshaller()
val queryGenerator = new QueryGenerator(TestSchema.schema)
val variableGenerator = new VariableGenerator(TestSchema.schema)
val getUserCall = QueryByName("getUser")
val getUserCallArguments = Map("userId" -> 1L)
val createUserCall = MutationByName("createUser")
val createUserCallArguments = Map[String, Any](
  "name" -> "test user",
  "age" -> Some(26),
  "address" -> Map[String, Any](
    "zip" -> 2300,
    "city" -> "cph",
    "street" -> "etv"
  ),
  "hobbies" -> List("coding", "debugging")
)
val createUserCallVariables = variableGenerator.generateVariables(
  TestSchema.Mutations.createUser,
  createUserCallArguments
).right.toOption.get
val result = Await.result(
  Executor.execute(
    TestSchema.schema,
    queryGenerator.generateQuery(Mutation(TestSchema.Mutations.createUser)).right.toOption.get,
    new UserRepo {
      def getUser(id: Long) = None

      def createUser(name: String, age: Option[Int], hobbies: List[String], address: Address) = {
        User(1L, name, age.getOrElse(100), hobbies, address)
      }
    },
    variables = createUserCallVariables
  ),
  5 seconds
)

val unmarshaller = new QueryAstInputUnmarshaller()

unmarshaller.render(ast.StringValue("test user"))
unmarshaller.render(ast.ObjectValue(
  ("name", ast.StringValue("test user")),
  ("age", ast.IntValue(26)),
  ("hobbies", ast.ListValue(Vector(ast.StringValue("coding"), ast.StringValue("debugging"))))
))

unmarshaller.render(
  variableGenerator.generateVariables(getUserCall, getUserCallArguments).right.toOption.get
)
unmarshaller.render(
  variableGenerator.generateVariables(createUserCall, createUserCallArguments).right.toOption.get
)

QueryRenderer.render(queryGenerator.generateQuery(QueryByName("getUser")).right.toOption.get)
QueryRenderer.render(queryGenerator.generateQuery(createUserCall).right.toOption.get)
