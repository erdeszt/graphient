## GraphQL client for Sangria schemas

[![Build Status](https://travis-ci.org/erdeszt/graphient.svg?branch=master)](https://travis-ci.org/erdeszt/graphient)


### Usage:

#### Add the package to your build:

```scala
resolvers += Resolver.bintrayRepo("erdeszt", "io.github.erdeszt")
libraryDependencies += "io.github.erdeszt" %% "graphient" % "4.0.1"
libraryDependencies += "io.github.erdeszt" %% "graphient-circe" % "1.0.0" // For circe support
libraryDependencies += "io.github.erdeszt" %% "graphient-spray" % "1.0.0" // For spray support
```

#### Using the client(with the circe encoder)

```scala
import graphient.GraphientClient
import graphient.circe._

// IMPORTANT: You also need to import your preferred sttp backend

// For the definition of the TestSchema check: graphient/src/test/scala/graphient/TestSchema.scala
val client = new GraphientClient(TestSchema.schema, uri"http://yourapi.com/graphql")

// Using the raw sttp api:
// `request` is a normal sttp request with the body set to the generated graphql query
// and content type set to application/json. You can add headers directly here or after the request is generated.
// You can also use any type that is encodable as the parameters
val request = client.createRequest(Query(TestSchema.Queries.getUser), Map("userId" -> 1L), "Authorization" -> "Bearer token")

// When you are ready, send the request to receive the response. You will need to
// decode the json response.
val response = request.send()

// Using the higher level api (with circe decoding):
// `responseData` is an F[User] where F is your effect type(has to support cats-effect `MonadError[F, Throwable]`).
// You can add extra headers the same way as in `createRequest` passing in a variable number of `(String, String)` args
// starting from position 3
// The imported sttp backend has to use the same F effect
val responseData = client.call[F, Params.T, TestSchema.Domain.User](Query(TestSchema.Queries.getUser), Map("userId" -> 1L))

```

#### Other modes

```scala
import graphient._
import graphient.model._

// Create a query & a variable generator based on some Sangria schema
// For the definition of the TestSchema check: graphient/src/test/scala/graphient/TestSchema.scala
val queryGenerator = new QueryGenerator(TestSchema)
val variableGenerator = new VariableGenerator(TestSchema)

// Generate a query by name
val queryByName = queryGenerator.generateQuery(QueryByName("getUser"))
// or by using the definition
val queryByDefinition = queryGenerator.generateQuery(Query(TestSchema.Qeries.getUser))

// Generate a mutation by name
val mutationByName = queryGenerator.generateQuery(MutationByName("createUser"))
// or by using the definition
val mutationByDefinition = queryGenerator.generateQuery(Mutation(TestSchema.Mutations.get.createUser))

// Generate variables for the query
val variablesForGetUser = variableGenerator.generateVariables(
  QueryByName("getUser"),
  Map[String, Any]("userId" -> 1L)
)
// and for the mutation
val variablesForCreateUser = variableGenerator.generateVariables(
  MutationByName("createUser"),
  Map[String, Any](
    "name"    -> "user 1",
    "age"     -> 26,
    "hobbies" -> List("coding", "debugging")
  )
)

// You can execute the queries in memory using a sangria.execution.Executor:
val result = Executor.execute(
  schema      = TestSchema,
  queryAst    = queryByName.right.toOption.get,
  userContext = context,
  variables   = variablesForGetUser.right.toOption.get
)

// or pretty print it with sangria.renderer.QueryRenderer
val renderedQuery = QueryRenderer.render(mutationByDefinition.right.toOption.get)
// =
// mutation ($name: String!, $age: Int!, $hobbies: [String!]!) {
//   createUser(name: $name, age: $age, hobbies: $hobbies) {
//     id
//     name
//     age
//     hobbies
//   }
// }

// along with the variables using the sangria.marshalling.QueryAstInputUnmarshaller
val unmarshaller = new QueryAstInputUnmarshaller()
val renderedVariables = unmarshaller.render(variablesForCreateUser.right.toOption.get)
// = {name:"user 1",age:26,hobbies:["coding","debugging"]}
```

For further examples check [graphient/src/test/scala/graphient/Worksheet.sc]() and the specs in [grahpeint/src/test]() and [graphient/src/it]().
