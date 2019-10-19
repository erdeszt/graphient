package graphient

import cats.data.NonEmptyList
import graphient.model._
import org.scalacheck._

trait Generators {

  // To generate non empty http header names and values
  case class Header(key: String, value: String)

  implicit val headerArb = Arbitrary {
    for {
      key <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString(""))
      value <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString(""))
    } yield Header(key, value)
  }

  implicit val graphqlCallArb: Arbitrary[GraphqlCall[_, _]] = {
    Arbitrary {
      Gen.oneOf(
        Gen.oneOf(TestSchema.Queries.schema.fields).map(Query(_)):      Gen[GraphqlCall[_, _]],
        Gen.oneOf(TestSchema.Mutations.schema.fields).map(Mutation(_)): Gen[GraphqlCall[_, _]]
      )
    }
  }

  implicit def nonEmptyListArb[A](implicit arb: Arbitrary[A]): Arbitrary[NonEmptyList[A]] = {
    Arbitrary {
      for {
        head <- arb.arbitrary
        tail <- Gen.listOf(arb.arbitrary)
      } yield NonEmptyList(head, tail)
    }
  }

  implicit val graphqlResponseErrorLocationArb: Arbitrary[GraphqlResponseErrorLocation] = {
    Arbitrary {
      for {
        line <- Gen.chooseNum[Int](0, 100)
        column <- Gen.chooseNum[Int](0, 100)
      } yield GraphqlResponseErrorLocation(line, column)
    }
  }

  implicit val graphqlResponseErrorArb: Arbitrary[GraphqlResponseError] = {
    Arbitrary {
      for {
        message <- Gen.alphaNumStr
        path <- Gen.listOf(Gen.alphaNumStr)
        locations <- Gen.listOf(graphqlResponseErrorLocationArb.arbitrary)
      } yield GraphqlResponseError(message, path, locations)
    }
  }

}
