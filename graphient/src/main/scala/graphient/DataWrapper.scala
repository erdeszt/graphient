package graphient

import io.circe.{Decoder, HCursor}

case class DataWrapper[T](fieldName: String, value: T)

object DataWrapper {
  def createDecoder[T](fieldName: String)(implicit decoder: Decoder[T]): Decoder[DataWrapper[T]] = {
    new Decoder[DataWrapper[T]] {
      def apply(c: HCursor): Decoder.Result[DataWrapper[T]] = {
        val field   = c.downField(fieldName)
        val payload = decoder.tryDecode(field)

        payload.map(DataWrapper(fieldName, _))
      }
    }
  }
}
