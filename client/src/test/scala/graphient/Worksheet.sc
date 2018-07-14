import graphient.GraphqlCall.Query
import graphient.TestSchema._
import graphient._
import sangria.renderer._
import sangria.execution._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

val schema = TestSchema()
val testClient = Client(schema)
val query = testClient.call(Query("getUser"), Map("userId" -> 1L)).getOrElse(throw new Exception("X"))

Await.result(Executor.execute(schema, query, new UserRepo {
  override def getUser(id: Long) = {
    Some(User(id, s"User: $id", 25 + id.toInt))
  }
}), 5 seconds)

QueryRenderer.render(query)