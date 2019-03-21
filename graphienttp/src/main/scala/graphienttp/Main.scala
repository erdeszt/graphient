package graphienttp
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import graphient.{Query, TestSchema}
import graphient.TestSchema.Domain.UserRepo

import com.softwaremill.sttp._

object Main {
  def main(args: Array[String]): Unit = {

    // TODO: provide sttp backend

    println("ping")

    implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

    val client = new GraphienttpClient[UserRepo](TestSchema.schema, uri"http://localhost:8080/graphql")

    val request: Request[String, Nothing] =
      client.runQuery(Query(TestSchema.Queries.getUser), Map[String, Any]("userId" -> 1L))

    val response = request.send()
    println(response)

  }
}
