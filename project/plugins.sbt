logLevel := Level.Warn

//Plugin for Scapegoat

addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.0.4")

//Plugin for Scalastyle

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

//Plugin for Scoverage

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

//Plugin for Copy Paste Detector

addSbtPlugin("de.johoop" %% "cpd4sbt" % "1.2.0")

// Plugin to format scala code while compilation
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// Plugins to make Jar
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")