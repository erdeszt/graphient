package graphient

import cats.{Id, MonadError}

object IdMonadError {

  implicit val idMonadError = new MonadError[Id, Throwable] {
    def raiseError[A](e:       Throwable): Id[A] = throw e
    def handleErrorWith[A](fa: Id[A])(f: Throwable => Id[A]): Id[A] = {
      try (fa)
      catch { case ex: Throwable => f(ex) }
    }
    def pure[A](x:        A): Id[A] = x
    def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
    def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] = {
      f(a) match {
        case Left(a)  => tailRecM[A, B](a)(f)
        case Right(b) => b
      }
    }
  }

}
