package com.evvo.remoting

import java.io.File

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object RemoteActorSystem {
  def main(args: Array[String]): Unit = {

    val configFile = ConfigFactory.parseFile(new File("src/main/resources/application.conf"))
    val config = configFile
      .getConfig("RemoteActorSystem")
      .withFallback(configFile)
      .resolve()

    implicit val system: ActorSystem = ActorSystem("RemoteEvvoNode", config)
  }
}
