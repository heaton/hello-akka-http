package me.heaton

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.io.StdIn

object WebServer {
  //  def main(args: Array[String]) {
  //
  //    implicit val system = ActorSystem("my-system")
  //    implicit val materializer = ActorMaterializer()
  //    // needed for the future flatMap/onComplete in the end
  //    implicit val executionContext = system.dispatcher
  //
  //    val route =
  //      path("hello") {
  //        get {
  //          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
  //        }
  //      }
  //
  //    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  //
  //    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  //    StdIn.readLine() // let it run until user presses return
  //    bindingFuture
  //      .flatMap(_.unbind()) // trigger unbinding from the port
  //      .onComplete(_ => system.terminate()) // and shutdown when done
  //  }

  // needed to run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher

  var orders: List[Item] = Nil

  // domain model
  final case class Item(name: String, id: Long) {
    require(!name.isEmpty, "name can not be empty")
    require(id > 0, "id has to be an positive integer")
  }

  final case class Order(items: List[Item])

  // formats for unmarshalling and marshalling
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)

  // (fake) async database query api
  def fetchItem(itemId: Long): Future[Option[Item]] = Future {
    orders.find(o => o.id == itemId)
  }

  def saveOrder(order: Order): Future[Done] = {
    orders = order match {
      case Order(items) => items ::: orders
      case _ => orders
    }
    Future {
      Done
    }
  }

  def oauth2Athenicator(credentials: Credentials): Option[String] =
    credentials match {
      case p@Credentials.Provided(token) if p.verify("Heaton") => Some(token)
      case _ => None
    }

  val routes: Route =
    get {
      pathPrefix("item" / LongNumber) { id =>
        // there might be no item for a given id
        val maybeItem: Future[Option[Item]] = fetchItem(id)

        onSuccess(maybeItem) {
          case Some(item) => complete(item)
          case None => complete(StatusCodes.NotFound)
        }
      }
    } ~
    post {
      path("create-order") {
        authenticateOAuth2("hello akka", oauth2Athenicator) { _ =>
          entity(as[Order]) { order =>
            val saved: Future[Done] = saveOrder(order)
            onComplete(saved) { done =>
              complete("order created")
            }
          }
        }
      }
    }

  def main(args: Array[String]) {

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done

  }

}
