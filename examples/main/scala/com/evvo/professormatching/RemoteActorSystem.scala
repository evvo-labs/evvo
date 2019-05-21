package com.evvo.professormatching

import java.io.File

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.config._

object RemoteActorSystem {
  def main(args: Array[String]): Unit = {

    val config = ConfigFactory
      .parseFile(new File("src/main/resources/remote.conf"))
      .withFallback(ConfigFactory.parseFile(new File("src/main/resources/application.conf")))
      .resolve()
    implicit val system: ActorSystem = ActorSystem("RemoteEvvoNode", config)
  }
}
