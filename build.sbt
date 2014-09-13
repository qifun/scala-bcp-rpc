resolvers in ThisBuild += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers in ThisBuild += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.qifun" %% "stateless-future-util" % "0.5.0"

libraryDependencies += "com.qifun" %% "json-stream" % "0.1.0"

libraryDependencies += "com.qifun" %% "scala-bcp" % "0.1.0"

scalacOptions += "-feature"

crossScalaVersions := Seq("2.11.2")
