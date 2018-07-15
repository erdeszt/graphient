package graphient

import TestSchema.Domain._
import org.scalatest._
import sangria.validation.QueryValidator

class QueryGeneratorSpec extends FunSpec with Matchers {

  val queryGenerator = QueryGenerator[UserRepo, Unit](TestSchema.schema)

  describe("QueryGenerator") {

    // TODO: Rename v1/v2 to something more meaningful
    describe("V1 api") {

      ignore("should handle missing fields") {
        fail("WIP")
      }

    }

    describe("V2 api") {

      it("should generate a valid query ast for queries") {
        val queryAst   = queryGenerator.generateQuery(Query(TestSchema.Queries.getUser))
        val violations = QueryValidator.default.validateQuery(TestSchema.schema, queryAst)

        violations shouldBe empty
      }

      it("should generate a valid query ast for mutations") {
        val queryAst   = queryGenerator.generateQuery(Mutation(TestSchema.Mutations.createUser))
        val violations = QueryValidator.default.validateQuery(TestSchema.schema, queryAst)

        violations shouldBe empty
      }

      ignore("should not allow queries to be called as mutations") {
        intercept {
          queryGenerator.generateQuery(Query(TestSchema.Mutations.createUser))
        }
      }

    }

  }

}
