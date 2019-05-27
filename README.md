[![Build Status](https://travis-ci.org/emartech/jwt-akka-http.svg?branch=master)](https://travis-ci.org/emartech/jwt-akka-http) [![Maven Central](https://img.shields.io/maven-central/v/com.emarsys/jwt-akka-http_2.12.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.emarsys%22%20AND%20a:%22jwt-akka-http_2.12%22)

# jwt-akka-http
This is a scala library for akka-http that indroduces a directive to make JWT authentication easier.

## Usage

Add the following to `build.sbt`:
```scala
libraryDependencies += "com.emarsys" %% "jwt-akka-http" % "1.0.1+6-9e96f102"
```

In your code import and mixin the `com.emarsys.authentication.jwt.JwtAuthentication` trait and override its `jwtConfig` member with a valid configuration.
A JwtConfig can be created using a `com.typesafe.config.Config` which contains the configuration keys:
```scala
expirationTime: Duration
secret: String
```

This can be achieved with a section like this in the `application.config` file:
```
expiration-time: 3 minutes
secret: "shhh, this is a secret!"
```

## Examples

Returning a new token containing any custom data defined as a case class:
```scala
(get & path("handshake")) {
  val customTokenData = CustomTokenData(customerId, name)
  complete(generateToken(customTokenData))
}
```

Authenticating a token received from the client:
```scala
(get & path("customers")) {
  jwtAuthenticate(as[CustomTokenData]) { _ =>
    complete(StatusCodes.OK)
  }
}
```

Authenticating a token and the custom contents:
```scala
(get & path("customers" / IntNumber)) { requestedCustomerId =>
  jwtAuthenticate(as[CustomTokenData]) { customTokenData =>
    if (customTokenData.customerId == requestedCustomerId) {
      complete(StatusCodes.OK)
    }
  }
}
```

## License

See the attached [LICENSE](LICENSE) file.
