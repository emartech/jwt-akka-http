package com.emarsys.jwt.akka.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Inside
import pdi.jwt.{Jwt, JwtClaim}
import spray.json._

import scala.util.Success
import org.scalatest.matchers.should.Matchers
import org.scalatest.verbs.ShouldVerb
import org.scalatest.wordspec.AnyWordSpec

class JwtAuthenticationSpec
    extends AnyWordSpec
    with Matchers
    with ShouldVerb
    with JwtAuthentication
    with ScalatestRouteTest
    with SprayJsonSupport
    with Inside {

  val config: Config = ConfigFactory.load()
  override val jwtConfig: JwtConfig = new JwtConfig(config.getConfig("jwt"))

  implicit val clock = java.time.Clock.systemUTC()

  "JwtAuthentication directive" when {
    "using a valid jwt token" should {
      "return OK" in {
        val claim = defaultClaim.expiresIn(200)
        val validToken = encodeToken(claim, jwtConfig.secret)
        Get("/") ~> withJwtToken(validToken) ~> jwtAuthenticate(as[ClaimData]) { _ =>
          complete(StatusCodes.OK)
        } ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "using no jwt token" should {
      "reject with missing credentials" in {
        Get("/") ~> jwtAuthenticate(as[ClaimData]) { _ =>
          complete(StatusCodes.OK)
        } ~> check {
          rejection shouldBe an[AuthenticationFailedRejection]
          rejection.asInstanceOf[AuthenticationFailedRejection].cause shouldBe AuthenticationFailedRejection.CredentialsMissing
        }
      }
    }

    "using an invalid jwt token" should {
      "reject with wrong credentials" in {
        val invalidToken = "invalid token"
        Get("/") ~> withJwtToken(invalidToken) ~> jwtAuthenticate(as[ClaimData]) { _ =>
          complete(StatusCodes.OK)
        } ~> check {
          rejection shouldBe an[AuthenticationFailedRejection]
          rejection.asInstanceOf[AuthenticationFailedRejection].cause shouldBe AuthenticationFailedRejection.CredentialsRejected
        }
      }
    }

    "using a jwt token with invalid secret" should {
      "reject with wrong credentials" in {
        val claim = defaultClaim.expiresIn(200)
        val invalidToken = encodeToken(claim, "invalid secret")
        Get("/") ~> withJwtToken(invalidToken) ~> jwtAuthenticate(as[ClaimData]) { _ =>
          complete(StatusCodes.OK)
        } ~> check {
          rejection shouldBe an[AuthenticationFailedRejection]
          rejection.asInstanceOf[AuthenticationFailedRejection].cause shouldBe AuthenticationFailedRejection.CredentialsRejected
        }
      }
    }

    "using a jwt token that is not yet valid" should {
      "reject with wrong credentials" in {
        val claim = defaultClaim.startsIn(200).expiresIn(400)
        val invalidToken = encodeToken(claim, jwtConfig.secret)
        Get("/") ~> withJwtToken(invalidToken) ~> jwtAuthenticate(as[ClaimData]) { _ =>
          complete(StatusCodes.OK)
        } ~> check {
          rejection shouldBe an[AuthenticationFailedRejection]
          rejection.asInstanceOf[AuthenticationFailedRejection].cause shouldBe AuthenticationFailedRejection.CredentialsRejected
        }
      }
    }

    "using an expired jwt token" should {
      "reject with wrong credentials" in {
        val claim = defaultClaim.expiresNow
        val expiredToken = encodeToken(claim, jwtConfig.secret)
        Get("/") ~> withJwtToken(expiredToken) ~> jwtAuthenticate(as[ClaimData]) { _ =>
          complete(StatusCodes.OK)
        } ~> check {
          rejection shouldBe an[AuthenticationFailedRejection]
          rejection.asInstanceOf[AuthenticationFailedRejection].cause shouldBe AuthenticationFailedRejection.CredentialsRejected
        }
      }
    }

    "using a valid jwt token" should {
      "pass the jwt claim data" in {
        val claim = defaultClaim.expiresIn(200)
        val validToken = encodeToken(claim, jwtConfig.secret)
        Get("/") ~> withJwtToken(validToken) ~> jwtAuthenticate(as[ClaimData]) { claimData =>
          complete(claimData)
        } ~> check {
          val resultClaimData = responseAs[ClaimData]
          resultClaimData shouldEqual extractClaimData(claim)
        }
      }
    }
  }


  "Jwt token generator" when {
    "given a user data case class" should {
      "generates a token using the case class as claim" in {
        val token = generateToken(claimData)
        val decodedTokenTry = Jwt.decode(token, jwtConfig.secret, Seq(encodingAlgorithm))

        inside(decodedTokenTry) {
          case Success(decodedToken) =>
            val tokenFields = decodedToken.content.parseJson.asJsObject.fields
            tokenFields.get("data").map(_.toString) should contain("\"data\"")
        }
      }
    }
  }

  case class ClaimData(data: String)
  import DefaultJsonProtocol._
  implicit val claimFormat = jsonFormat1(ClaimData.apply)

  private val claimData = ClaimData("data")

  private val defaultClaim: JwtClaim = JwtClaim(claimData.toJson.toString)

  private def encodeToken(claim: JwtClaim, secret: String) =
    Jwt.encode(claim, secret, encodingAlgorithm)

  private def withJwtToken(jwtToken: String) =
    addHeader(Authorization(OAuth2BearerToken(jwtToken)))

  private def extractClaimData(jwtClaim: JwtClaim) =
    jwtClaim.toJson.parseJson.convertTo[ClaimData]
}
