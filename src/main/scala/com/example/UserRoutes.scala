package com.example

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import com.example.InMemoryUserRepository._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[InMemoryUserRepository.Command])
                (implicit val system: ActorSystem[_]) {
  // #user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  // #import-json-formats
  val userRoutes: Route =
    pathPrefix("users") {
        concat(pathEndUsers, getAUser, createNewUser)
    }

  val pathEndUsers: Route = pathEnd {
    get {complete(StatusCodes.OK, getUsers)}
  }
  val createNewUser: Route = post {
    entity(as[User]) { user =>
      onSuccess(createUser(user)) { performed =>
        complete((StatusCodes.Created, performed))
      }
    }
  }

  val getAUser: Route = get {path(IntNumber) {int => complete(StatusCodes.OK, s"OK GOOD $int")}}

  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def getUsers: Future[Users] = userRegistry.ask(GetUsers)
  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))


}
