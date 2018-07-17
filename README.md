## GraphQL client for Sangria schemas

[![Build Status](https://travis-ci.org/erdeszt/graphient.svg?branch=master)](https://travis-ci.org/erdeszt/graphient)


### Usage:
```scala
import graphient._

// Create a query & a varaible generator based on some Sangria schema
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

For further examples check `graphient/src/test/scala/graphient/Worksheet.sc` and the specs.