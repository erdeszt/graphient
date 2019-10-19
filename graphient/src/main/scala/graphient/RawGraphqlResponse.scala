package graphient

case class RawGraphqlResponse[T](
    data:   Option[Map[String, T]],
    errors: Option[List[GraphqlResponseError]]
)
