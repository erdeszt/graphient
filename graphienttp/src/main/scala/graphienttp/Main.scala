package graphienttp
import cats.effect.ExitCase.Completed
import cats.effect.{Async, ExitCase, Sync}
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import graphient.{Query, TestSchema}
import graphient.TestSchema.Domain.UserRepo
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import io.circe._
import cats.implicits._
import cats.instances.future._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.Failure

object Main {

  // Note: recall alt + enter to set up alla the boiler plate form implementations
  implicit val syncForFuture = new Async[Future] {
    override def suspend[A](thunk:          => Future[A]): Future[A] = thunk
    override def bracketCase[A, B](acquire: Future[A])(use: A => Future[B])(
        release:                            (A, ExitCase[Throwable]) => Future[Unit]): Future[B] = {
      acquire.flatMap { resource =>
        use(resource)
          .flatMap { b =>
            release(resource, Completed).map(_ => b)
          }
          .recoverWith {
            case e: Throwable =>
              release(resource, ExitCase.error(e)).flatMap(_ => Future.failed(e))
          }
      }
    }
    override def raiseError[A](e:       Throwable): Future[A] = Future(throw e)
    override def handleErrorWith[A](fa: Future[A])(
        f:                              Throwable => Future[A]
    ): Future[A] = fa.recoverWith { case e: Throwable => f(e) }
    override def pure[A](x:        A): Future[A] = Future(x)
    override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)
    override def tailRecM[A, B](a: A)(f: A => Future[Either[A, B]]): Future[B] = {
      f(a).flatMap { x =>
        x match {
          case Right(r) => Future(r)
          case Left(l)  => tailRecM(l)(f)
        }
      }
    }

    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Future[A] = {
      val p = Promise[A]()
      val f = p.future

      k(eitherThrowableA => {
        eitherThrowableA match {
          case Left(error)  => p.failure(error)
          case Right(value) => p.success(value)
        }
      })
      f
    }
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => Future[Unit]): Future[A] = {
      val p = Promise[A]()
      val f = p.future
      k(eitherThrowableA => {
        eitherThrowableA match {
          case Left(error)  => p.failure(error)
          case Right(value) => p.success(value)
        }
      }).flatMap(_ => f)
    }
  }

  implicit val backend = AkkaHttpBackend()

  implicit val mapStringAnyEncoder: Encoder[Map[String, Any]] = { _ =>
    Json.fromFields(List("userId" -> Json.fromLong(1L)))
  }

  def main(args: Array[String]): Unit = {

    println("ping")

    val client =
      new GraphienttpClient(TestSchema.schema, uri"http://localhost:8080/graphql")

    // TODO: Map[String, Any] based implementation for convenience
    val response =
      client.runQuery(Query(TestSchema.Queries.getUser), Map[String, Any]("userId" -> 1L)) // GetUserPayload(42L))

    response.onComplete {
      case scala.util.Success(r) => println(s"response: $r")
      case Failure(error)        => println(s"error: $error")
    }

  }
}
