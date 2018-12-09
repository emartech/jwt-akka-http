package com.emarsys.jwt.akka.http

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import spray.json.{JsonWriter, _}

import scala.util.{Failure, Success, Try}

trait JwtAuthentication {
  val jwtConfig: JwtConfig

  private final val tokenPrefix = "Bearer "

  protected val encodingAlgorithm: JwtAlgorithm.HS256.type = JwtAlgorithm.HS256
  private val acceptedAlgorithms = Seq(encodingAlgorithm)

  def generateToken[UserData: JsonWriter](userData: UserData): String = {
    val userDataJson = userData.toJson.toString
    val claim = JwtClaim(userDataJson).expiresIn(jwtConfig.expirationTime.getSeconds)
    Jwt.encode(claim, jwtConfig.secret, encodingAlgorithm)
  }

  def jwtAuthenticate[UserData](um: FromStringUnmarshaller[UserData]): Directive1[UserData] = for {
    jwtToken <- optionalHeaderValueByName("Authorization").map(stripBearerPrefix)
    userData <- jwtAuthenticateToken(jwtToken, um)
  } yield userData

  def jwtAuthenticateToken[UserData](jwtToken: Option[String], um: FromStringUnmarshaller[UserData]): Directive1[UserData] = for {
    authorizedToken <- checkAuthorization(jwtToken)
    decodedToken <- decodeToken(authorizedToken)
    userData <- convertToUserData(decodedToken, um)
  } yield userData

  def as[UserData](implicit um: FromStringUnmarshaller[UserData]): FromStringUnmarshaller[UserData] = um

  private def stripBearerPrefix(token: Option[String]): Option[String] = {
    token.map(_.stripPrefix(tokenPrefix))
  }

  private def checkAuthorization[UserData](a: Option[String]): Directive1[String] = a match {
    case Some(jwt) if isValid(jwt) => provide(jwt)
    case None => reject(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, HttpChallenge("Basic", "JWT")))
    case _ => reject(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, HttpChallenge("Basic", "JWT")))
  }

  private def isValid(jwt: String) = {
    Jwt.isValid(jwt, jwtConfig.secret, acceptedAlgorithms)
  }

  private def decodeToken[UserData](jwt: String): Directive1[String] = {
    provide(Jwt.decode(jwt, jwtConfig.secret, acceptedAlgorithms).get)
  }

  private def convertToUserData[UserData](decodedToken: String, um: FromStringUnmarshaller[UserData]): Directive1[UserData] = {
    extractExecutionContext.flatMap { implicit ctx =>
      extractMaterializer.flatMap { implicit mat =>
        onComplete(um(decodedToken)).flatMap(handleError)
      }
    }
  }

  private def handleError[UserData](unmarshalledUserData: Try[UserData]): Directive1[UserData] = unmarshalledUserData match {
    case Success(value) => provide(value)
    case Failure(RejectionError(r)) ⇒ reject(r)
    case Failure(Unmarshaller.NoContentException) ⇒ reject(RequestEntityExpectedRejection)
    case Failure(Unmarshaller.UnsupportedContentTypeException(x)) ⇒ reject(UnsupportedRequestContentTypeRejection(x))
    case Failure(x: IllegalArgumentException) ⇒ reject(ValidationRejection(blankIfNull(x.getMessage), Some(x)))
    case Failure(x) ⇒ reject(MalformedRequestContentRejection(blankIfNull(x.getMessage), x))
  }

  private def blankIfNull(str: String): String = if(str == null) "" else str
}
