package graphient.serializer

trait Encoder[T] {
  def encode(requestBody: T): String
}
