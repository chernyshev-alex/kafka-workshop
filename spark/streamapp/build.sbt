// give the user a nice default project!

lazy val root = (project in file(".")).

  settings(
    inThisBuild(List(
      organization := "com.example.acme",
      scalaVersion := "2.11.8"
    )),
    name := "streamApp",
    version := "0.0.1",

    sparkVersion := "2.4.4",
    sparkComponents := Seq(),

    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled"),
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    parallelExecution in Test := false,
    fork := true,

    coverageHighlighting := true,

    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-streaming" % "2.4.4" % "provided",
      "org.apache.spark" %% "spark-sql" % "2.4.4" % "provided",
      "za.co.absa" %% "abris" % "3.0.3" % "provided",
      "org.apache.spark" %% "spark-avro" % "2.4.4" % "provided",
      "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.4.4" % "provided",
      "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.4" % "provided", 
      "com.lihaoyi" %% "ujson" % "0.7.1",
   	  "com.lihaoyi" %% "requests" % "0.1.8",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
      "com.holdenkarau" %% "spark-testing-kafka-0_8" % "2.3.1_0.12.0" % "test" 
    ),

    // uses compile classpath for the run task, including "provided" jar (cf http://stackoverflow.com/a/21803413/3827)
    run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)).evaluated,

    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    pomIncludeRepository := { x => false },

   resolvers ++= Seq(
      "confluent" at  "http://packages.confluent.io/maven/",
      "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/",
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Second Typesafe repo" at "http://repo.typesafe.com/typesafe/maven-releases/",
      Resolver.sonatypeRepo("public")
    ),

    pomIncludeRepository := { x => false },

    // publish settings
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  )
