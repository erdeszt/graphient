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

// Query and variable generators
val queryGenerator = new QueryGenerator(TestSchema.schema)
val variableGenerator = new VariableGenerator(TestSchema.schema)

// Some example queries and mutations with their arguments
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

// Generate the mutation for createUser
val createUserMutation = queryGenerator.generateQuery(Mutation(TestSchema.Mutations.createUser)).right.toOption.get

// Generate variables for the createUser mutation
val createUserCallVariables = variableGenerator.generateVariables(
  TestSchema.Mutations.createUser,
  createUserCallArguments
).right.toOption.get

// Run the mutation locally with a fake service implementation
val result = Await.result(
  Executor.execute(
    TestSchema.schema,
    createUserMutation,
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

// Query AST renderer examples
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

// Generate query for the example query and mutation
QueryRenderer.render(queryGenerator.generateQuery(QueryByName("getUser")).right.toOption.get)
QueryRenderer.render(queryGenerator.generateQuery(createUserCall).right.toOption.get)

QueryRenderer.render(queryGenerator.generateQuery(Query(TestSchema.Queries.getLong)).right.get)
