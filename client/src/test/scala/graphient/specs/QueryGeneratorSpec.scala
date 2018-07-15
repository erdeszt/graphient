package graphient.specs

import graphient._
import graphient.TestSchema.Domain._
import org.scalatest._
import sangria.validation.QueryValidator

class QueryGeneratorSpec extends FunSpec with Matchers {

  val queryGenerator = QueryGenerator[UserRepo, Unit](TestSchema.schema)

  describe("QueryGenerator") {

    describe("call by field api") {

      it("should generate a valid query ast for queries") {
        val queryAst = queryGenerator.generateQuery(Query(TestSchema.Queries.getUser))

        queryAst should be('right)

        val violations = QueryValidator.default.validateQuery(TestSchema.schema, queryAst.right.toOption.get)

        violations shouldBe empty
      }

      it("should generate a valid query ast for mutations") {
        val queryAst = queryGenerator.generateQuery(Mutation(TestSchema.Mutations.createUser))

        queryAst should be('right)

        val violations = QueryValidator.default.validateQuery(TestSchema.schema, queryAst.right.toOption.get)

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
