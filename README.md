## GraphQL client for Sangria schemas

[![Build Status](https://travis-ci.org/erdeszt/graphient.svg?branch=master)](https://travis-ci.org/erdeszt/graphient)

Library for generating and executing Graphql queries based on [Sangria](https://github.com/sangria-graphql/sangria) schemas.

### Usage:

#### Add the packages to your build:

```scala
resolvers += Resolver.bintrayRepo("erdeszt", "io.github.erdeszt")
libraryDependencies += "io.github.erdeszt" %% "graphient" % "4.0.1"
libraryDependencies += "io.github.erdeszt" %% "graphient-circe" % "1.0.0" // For circe support
libraryDependencies += "io.github.erdeszt" %% "graphient-spray" % "1.0.0" // For spray support
```

#### Using the high level client

##### Import the graphient package and your favorite serializer:

```scala
// Graphient client
import graphient._
import graphient.model._
// Graphient serializer
import graphient.serializer.circe._  // for circe support
// or
import graphient.serializer.spray._  // for spray support
```

##### Import your favorite sttp backend(in this example we'll use `AsyncHttpClientCatsBackend` with `IO` from `cats-effect`)

Add the backend to your build:
```scala
libraryDependencies += "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % "1.6.7"
```

_Graphient uses sttp `1.6.7` so your backend has to be compatible with it._

Create an implicit backend(the effect type has to support `MonadError[?[_], Throwable]` from `cats-effect`):

```scala
import cats.effect.IO
import sttp.client.asynchttpclient.cats._

implicit val sttpBackend = AsyncHttpClientCatsBackend[IO]()
```

##### Create a Graphient client with your schema ([TestSchema definition](https://github.com/erdeszt/graphient/blob/master/graphient/src/test/scala/graphient/TestSchema.scala)):

```scala
import com.softwaremill.sttp._ // For the `uri` string interpolator

val client = new GraphientClient(TestSchema.schema, uri"http://yourapi.com/graphql")
```

##### Executing the queries with the client

 * execute via the `GraphientClient`
 
    ```scala
    val query = Query(TestSchema.Queries.getUser)
    val parameters = Map("userId", 1)
    val responseData = client.call[TestSchema.Domain.User](query, parameters)
    ```
    `responseData` is an `IO[TestSchema.Domain.User]`, errors are propagated using `MonadError` from `cats-effect`
 * generate the sttp request and execute it manually 
   
   ```scala
   val query = Query(TestSchema.Queries.getUser)
   val parameters = Map("userId", 1)
   val request = client.createRequest(query, parameters)
   val response = request.toOption.get.send() // Do not call .toOption.get in real code!
   ```
   `request` is an `Either[GraphqlCallError, Request[String, Nothing]]`, you have to unwrap the `Either` and execute the request manually
   
   `response` is an `IO[Response[String]]`

##### Adding extra headers

Both `call` and `createRequest` supports adding extra headers to the request by passing in a varying number of `(String, String)` values starting from position 3
```scala
client.createRequest(query, parameters, ("Authorization", "Bearer mytoken"))
client.call(query, parameters, ("Authorization", "Bearer mytoken"))
```

#### Other modes

```scala
import graphient._
import graphient.model._
```

Create a query & a variable generator based on some Sangria schema
```scala
val queryGenerator = new QueryGenerator(TestSchema)
val variableGenerator = new VariableGenerator(TestSchema)
```

Generate a query or a mutation using either the schema directly or the name of the query:
```scala
val queryByDefinition = queryGenerator.generateQuery(Query(TestSchema.Qeries.getUser))
val queryByName = queryGenerator.generateQuery(QueryByName("getUser"))
val mutationByDefinition = queryGenerator.generateQuery(Mutation(TestSchema.Mutations.get.createUser))
val mutationByName = queryGenerator.generateQuery(MutationByName("createUser"))
```

Generate variables for the query and the mutation
```scala
val variablesForGetUser = variableGenerator.generateVariables(
  QueryByName("getUser"),
  Map[String, Any]("userId" -> 1L)
)
val variablesForCreateUser = variableGenerator.generateVariables(
  MutationByName("createUser"),
  Map[String, Any](
    "name"    -> "user 1",
    "age"     -> 26,
    "hobbies" -> List("coding", "debugging")
  )
)
```

You can execute the queries in memory using a sangria.execution.Executor:
```scala
val result = Executor.execute(
  schema      = TestSchema,
  queryAst    = queryByName.right.toOption.get,
  userContext = context,
  variables   = variablesForGetUser.right.toOption.get
)
```

Or pretty print it with sangria.renderer.QueryRenderer along with the variables using the sangria.marshalling.QueryAstInputUnmarshaller
```scala
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

val unmarshaller = new QueryAstInputUnmarshaller()
val renderedVariables = unmarshaller.render(variablesForCreateUser.right.toOption.get)
// = {name:"user 1",age:26,hobbies:["coding","debugging"]}
```

For further examples check [graphient/src/test/scala/graphient/Worksheet.sc]() and the specs in [grahpeint/src/test]() and [graphient/src/it]().
