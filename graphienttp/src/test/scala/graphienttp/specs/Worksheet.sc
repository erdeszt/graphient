import com.softwaremill.sttp._

// sttp basic example

val sort: Option[String] = None
val query = "http language:scala"

// the `query` parameter is automatically url-encoded
// `sort` is removed, as the value is not defined
// val request = sttp.get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")
val request = sttp.get(uri"http://localhost:8080")

implicit val backend = HttpURLConnectionBackend()
val response = request.send()

// response.header(...): Option[String]
println(response.header("Content-Length"))

// response.unsafeBody: by default read into a String
println(response.unsafeBody)
