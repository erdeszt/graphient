package graphient

import org.scalatest._

class ExecutionSpec extends FunSpec with Matchers {

  val queryGenerator    = QueryGenerator(TestSchema.schema)
  val variableGenerator = VariableGenerator(TestSchema.schema)

  describe("Local query execution with mock service") {

    ignore("should execute queries successfully") {
      // val query = queryGenerator.generateQuery()
    }

    ignore("should execute mutations successfully") {
      fail("WIP")
    }

  }

}
