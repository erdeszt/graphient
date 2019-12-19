package graphient.serializer

trait Decoder[T] {
  def decode(responseBody: String): Either[Throwable, T]
}
