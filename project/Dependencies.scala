import sbt._
import sbt._

object Dependencies {

  val scala = "2.11.8"
  val akkaHttpVersion = "2.4.11"
  val titanVersion = "1.0.0"
  val json4sVersion = "3.3.0"

  val akkaHttp         =  "com.typesafe.akka"          %% "akka-http-experimental"         % akkaHttpVersion
  val akkaHttpTest     =  "com.typesafe.akka"          %% "akka-http-testkit-experimental" % "2.4.2-RC3"
  val akkaSlf4j        =  "com.typesafe.akka"          %% "akka-slf4j"                     % akkaHttpVersion
  val scalaTest        =  "org.scalatest"              %% "scalatest"                      % "2.2.6"
  val eventuateCore    =  "com.rbmhtechnology"         %% "eventuate-core"                 % "0.8-SNAPSHOT"
  val eventuateLogCass =  "com.rbmhtechnology"         %% "eventuate-log-cassandra"        % "0.8-SNAPSHOT"
  val eventuateLogLDB  =  "com.rbmhtechnology"         %% "eventuate-log-leveldb"          % "0.8-SNAPSHOT"
  val liftJson         =  "net.liftweb"                %% "lift-json"                      % "2.6"
  val json4sNative     =  "org.json4s"                 %% "json4s-native"                  % json4sVersion
  val json4sExt        =  "org.json4s"                 %% "json4s-ext"                     % json4sVersion
  val akkaHttpJson     =  "de.heikoseeberger"          %% "akka-http-json4s"               % "1.7.0"
  val log              =  "com.typesafe.scala-logging" %% "scala-logging"                  % "3.4.0"
  val logback          =  "ch.qos.logback"             %  "logback-classic"                % "1.1.7"
  val jansi            =  "org.fusesource.jansi"       %  "jansi"                          % "1.12"
  val mock             =  "org.mockito"                % "mockito-core"                    % "1.9.5"
  val twilio           =  "com.twilio.sdk"             % "twilio-java-sdk"                 % "6.3.0"
  val dseGraph         =  "com.datastax.cassandra"     % "dse-driver"                      % "1.0.0"
  val jwt              =  "io.igl"                     %% "jwt"                            % "1.2.0"
  val redis            =  "net.debasishg"              %% "redisreact"                     % "0.8"
  val amazonAwss3      =  "com.amazonaws"              % "aws-java-sdk-s3"                 % "1.11.33"
  val javaxMail        =  "javax.mail"                 % "mail"                            % "1.4"
  val akkaQuazrtz      =  "com.enragedginger"          %% "akka-quartz-scheduler"          % "1.5.0-akka-2.4.x"
  val akkaActor        =  "com.typesafe.akka"          %% "akka-actor"                     % akkaHttpVersion
  val solrj            =  "org.apache.solr"            % "solr-solrj"                      % "6.2.1"
  val awsDependencies  =  "com.amazonaws"              % "aws-java-sdk"                    % "1.11.150"

  val akkaDependencies: Seq[ModuleID] = Seq(akkaHttp, log, logback, jansi, akkaSlf4j)

  val json4sDependencies = Seq(json4sExt, json4sNative, akkaHttpJson)

  val databaseDependencies = Seq(eventuateCore, eventuateLogCass, eventuateLogLDB)

  val apiDependencies = akkaDependencies

  val commonModuleDependencies = akkaDependencies ++ json4sDependencies ++ Seq(liftJson, twilio,jwt,dseGraph,amazonAwss3, javaxMail , awsDependencies)

  val persistenceDependencies = akkaDependencies ++ databaseDependencies ++ Seq(dseGraph,redis,solrj)

  val spokDependencies = akkaDependencies

  val notificationDependencies = akkaDependencies

  val accountsDependencies = akkaDependencies

  val schedulerDependencies = akkaDependencies ++ Seq(akkaQuazrtz, akkaActor)

}
