import graphient.GraphqlCall.{Mutation, Query}
import graphient.TestSchema._
import graphient._
import graphient.ClientV2._
import sangria.ast
import sangria.renderer._
import sangria.execution._
import sangria.marshalling.QueryAstInputUnmarshaller

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

val schema = TestSchema()
val testClient = Client(schema)
val queryGenerator = QueryGenerator(schema)
val query = testClient.call(Query("getUser"), Map("userId" -> 1L)).right.toOption.get
val mutation = testClient.call(
  Mutation("createUser"),
  Map(
    "name" -> "test user",
    "age" -> 26,
    "hobbies" -> List("coding", "debugging")
  )
).right.toOption.get

Await.result(Executor.execute(schema, query, new UserRepo {
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

QueryRenderer.render(queryGenerator.generateQuery(Query("getUser")).right.toOption.get)
QueryRenderer.render(queryGenerator.generateQuery(Mutation("createUser")).right.toOption.get)
