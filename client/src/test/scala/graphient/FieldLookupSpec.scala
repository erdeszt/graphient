package graphient

import org.scalatest._

class FieldLookupSpec extends FunSpec with Matchers {

  object TestObject extends FieldLookup

  describe("FieldLookup") {

    it("should find existing query fields") {
      val lookupResult = TestObject.getField(TestSchema.schema, QueryByName("getUser"))

      lookupResult should be('right)
      lookupResult.right.toOption.get.name should be("getUser")
    }

    it("should report missing query fields") {
      val lookupResult = TestObject.getField(TestSchema.schema, QueryByName("undefined"))

      lookupResult should be(Left(FieldNotFound(QueryByName("undefined"))))
    }

    it("should find existing mutation fields") {
      val lookupResult = TestObject.getField(TestSchema.schema, MutationByName("createUser"))

      lookupResult should be('right)
      lookupResult.right.toOption.get.name should be("createUser")
    }

    it("should report missing mutation fields") {
      val lookupResult = TestObject.getField(TestSchema.schema, MutationByName("undefined"))

      lookupResult should be(Left(FieldNotFound(MutationByName("undefined"))))
    }

    it("should not lookup a query when a mutation is searched") {
      val lookupResult = TestObject.getField(TestSchema.schema, MutationByName("getUser"))

      lookupResult should be(Left(FieldNotFound(MutationByName("getUser"))))
    }

    it("should not lookup a mutation when a query is searched") {
      val lookupResult = TestObject.getField(TestSchema.schema, QueryByName("createUser"))

      lookupResult should be(Left(FieldNotFound(QueryByName("createUser"))))
    }

  }

}
