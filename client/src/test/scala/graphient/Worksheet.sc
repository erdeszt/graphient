import graphient._
import graphient.TestSchema.Domain._
import sangria.ast
import sangria.renderer._
import sangria.execution._
import sangria.marshalling.QueryAstInputUnmarshaller

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

val testClient = OldClient(TestSchema.schema)
val queryGenerator = QueryGenerator(TestSchema.schema)
val variableGenerator = VariableGenerator(TestSchema.schema)
val getUserCall = Query("getUser")
val getUserCallArguments = Map("userId" -> 1L)
val query = testClient.call(getUserCall, getUserCallArguments).right.toOption.get
val createUserCall = Mutation("createUser")
val createUserCallArguments = Map(
  "name" -> "test user",
  "age" -> 26,
  "hobbies" -> List("coding", "debugging")
)
val mutation = testClient.call(
  createUserCall,
  createUserCallArguments
).right.toOption.get

Await.result(Executor.execute(TestSchema.schema, query, new UserRepo {
  override def getUser(id: Long) = {
    Some(User(id, s"User: $id", 25 + id.toInt, List("coding", "debugging")))
  }

  override def createUser(name: String, age: Int, hobbies: List[String]) = {
    User(1L, name, age, hobbies)
  }
}), 5 seconds)

QueryRenderer.render(query)

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

QueryRenderer.render(queryGenerator.generateQuery(Query("getUser")).right.toOption.get)
QueryRenderer.render(queryGenerator.generateQuery(createUserCall).right.toOption.get)
