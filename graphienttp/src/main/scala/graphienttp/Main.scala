package graphienttp
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import graphient.{Query, TestSchema}
import graphient.TestSchema.Domain.UserRepo
import com.softwaremill.sttp._
import io.circe._

object Main {

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  implicit val mapStringAnyEncoder: Encoder[Map[String, Any]] = { _ =>
    Json.fromFields(List("userId" -> Json.fromLong(1L)))
  }

  def main(args: Array[String]): Unit = {

    println("ping")

    val client = new GraphienttpClient[UserRepo](TestSchema.schema, uri"http://localhost:8080/graphql")

    // TODO: Map[String, Any] based implementation for convenience
    val request: Request[String, Nothing] =
      client.runQuery(Query(TestSchema.Queries.getUser), Map[String, Any]("userId" -> 1L)) // GetUserPayload(42L))

    val response = request.send()

    println(response)

  }
}
