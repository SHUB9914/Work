name := "cassndraDemo"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-extras" % "3.0.0"
libraryDependencies += "com.typesafe" % "config" % "1.2.1"
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.2" % "test"
libraryDependencies +="org.mockito" % "mockito-core" % "1.9.5"