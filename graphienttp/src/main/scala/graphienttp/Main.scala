package graphienttp
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import graphient.{Query, TestSchema}
import graphient.TestSchema.Domain.UserRepo

import com.softwaremill.sttp._

object Main {

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  def main(args: Array[String]): Unit = {

    println("ping")

    val client = new GraphienttpClient[UserRepo](TestSchema.schema, uri"http://localhost:8080/graphql")

    // TODO: Map[String, Any] based implementation for convenience
    val request: Request[String, Nothing] =
      client.runQuery(Query(TestSchema.Queries.getUser), GetUserPayload(42L))

    val response = request.send()

    println(response)

  }
}
