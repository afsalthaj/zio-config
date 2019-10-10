package zio.config.examples

import zio.DefaultRuntime
import zio.config._, Config._
import zio.config.actions.ConfigDocs._

object ReadWriteReport extends App {

  case class Password(value: String)
  case class UserPwd(name: String, pwd: Option[Password], abc: Option[String], value: Option[XYZ])
  case class Token(value: String, clientid: String)
  case class XYZ(xyz: String, someInteger: Either[Int, String])

  type ProdConfig = Either[UserPwd, Token]

  // An example where user provides a description once and for all, and use it for read, write, report!
  val config: ConfigDescriptor[ProdConfig] =
    ((string("usr") ? "Example: some-user" |@|
      string("pwd").xmap(Password)(_.value).optional ? "sec" |@|
      string("jhi").optional ? "Ex: ghi" |@|
      (string("xyz") |@| int("abc").orElseEither(string("def")))(XYZ.apply, XYZ.unapply).optional ? "Ex: ha")(
      UserPwd.apply,
      UserPwd.unapply
    ) orElseEither
      (string("auth_token") |@| string("clientid"))(Token.apply, Token.unapply)) ? "Prod Config"

  val runtime = new DefaultRuntime {}

  val userNamePassword =
    Map(
      "usr" -> "v1",
      "pwd" -> "v2",
      "abc" -> "1",
      "xyz" -> "v3"
    )

  val source =
    ConfigSource.fromMap(userNamePassword)

  val result: ProdConfig =
    runtime.unsafeRun(read(config).provide(source))

  assert(
    result == Left(UserPwd("v1", Some(Password("v2")), None, Some(XYZ("v3", Left(1)))))
  )

  // want to write back the config ?
  assert(
    runtime.unsafeRun(write(config).provide(result)) ==
      Map(
        "usr" -> "v1",
        "pwd" -> "v2",
        "xyz" -> "v3",
        "abc" -> "1"
      )
  )

  assert(
    docs(config, Some(result)) ==
      Or(
        And(
          And(
            And(
              PathDetails(
                List("usr"),
                Some("v1"),
                List("value of type string", "Example: some-user", "Prod Config")
              ),
              PathDetails(
                List("pwd"),
                Some("v2"),
                List("value of type string", "optional value", "sec", "Prod Config")
              )
            ),
            PathDetails(
              List("jhi"),
              None,
              List("value of type string", "optional value", "Ex: ghi", "Prod Config")
            )
          ),
          And(
            PathDetails(
              List("xyz"),
              Some("v3"),
              List("value of type string", "optional value", "Ex: ha", "Prod Config")
            ),
            Or(
              PathDetails(
                List("abc"),
                Some("1"),
                List("value of type int", "optional value", "Ex: ha", "Prod Config")
              ),
              PathDetails(
                List("def"),
                None,
                List("value of type string", "optional value", "Ex: ha", "Prod Config")
              )
            )
          )
        ),
        And(
          PathDetails(List("auth_token"), None, List("value of type string", "Prod Config")),
          PathDetails(List("clientid"), None, List("value of type string", "Prod Config"))
        )
      )
  )
}
