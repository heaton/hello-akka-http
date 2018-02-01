package me.heaton

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class WebServerSpec extends WordSpec with Matchers with ScalatestRouteTest {

  import WebServer.{Item, orders, routes}

  "The web server" should {
    "return an item for GET request of /item" in {
      orders = Item("Heaton", 1) :: Nil
      Get("/item/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual """{"name":"Heaton","id":1}"""
      }
    }

    lazy val oauth2Token = OAuth2BearerToken("Heaton")

    "create an order for POST of /create-order" in {
      Post("/create-order", withJsonBody(
        """
          |{
          |  "items": [
          |    {"name":"Heaton","id":1}
          |  ]
          |}
          |""".stripMargin)
      ) ~> addCredentials(oauth2Token) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual "order created"
      }
    }

    "return 400 Bad request for empty string of name in POST of /create-order" in {
      Post("/create-order", withJsonBody(
        """
          |{
          |  "items": [
          |    {"name":"","id":1}
          |  ]
          |}
          |""".stripMargin)
      ) ~> addCredentials(oauth2Token) ~> Route.seal(routes) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] contains "name can not be empty"
      }
    }

    "return 400 Bad request for minus id in POST of /create-order" in {
      Post("/create-order", withJsonBody(
        """
          |{
          |  "items": [
          |    {"name":"Heaton","id":-1}
          |  ]
          |}
          |""".stripMargin)
      ) ~> addCredentials(oauth2Token) ~> Route.seal(routes) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] should include ("id has to be an positive integer")
      }
    }

    "return 401 unauthenicated when no auth2 token provided on POST of /create-order" in {
      Post("/create-order", withJsonBody("{}")) ~> Route.seal(routes) ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] should include ("The resource requires authentication")
        header[`WWW-Authenticate`].get.challenges.head === HttpChallenge("Bearer", Some("Hello akka"), Map("charset" → "UTF-8"))
      }
    }

    "return 401 unauthenicated when wrong auth2 token provided on POST of /create-order" in {
      Post("/create-order", withJsonBody("{}")) ~> addCredentials(OAuth2BearerToken("Something else")) ~> Route.seal(routes) ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] should include ("The supplied authentication is invalid")
        header[`WWW-Authenticate`].get.challenges.head === HttpChallenge("Bearer", Some("Hello akka"), Map("charset" → "UTF-8"))
      }
    }
  }

  private def withJsonBody(json: String) = HttpEntity(MediaTypes.`application/json`, json)
}
