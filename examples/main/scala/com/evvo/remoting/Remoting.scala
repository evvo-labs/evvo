package com.evvo.remoting

import java.io.File

import akka.actor.{ActorSystem, Address, AddressFromURIString, Deploy}
import akka.remote.RemoteScope
import akka.util.Timeout
import com.evvo.agent
import com.evvo.agent.defaults.DeleteWorstHalfByRandomObjective
import com.evvo.agent.{CreatorFunc, MutatorFunc}
import com.evvo.island.{EvvoIslandActor, EvvoIslandBuilder, IslandManager, TerminationCriteria}
import com.evvo.island.population.{Maximize, Objective, TParetoFrontier}
import com.typesafe.config.ConfigFactory

import collection.JavaConverters._
import scala.concurrent.duration._

object Remoting {
  def main(args: Array[String]): Unit = {
    val builder = EvvoIslandBuilder[Int]()
      .addCreator(CreatorFunc(() => Set(0), "Set(0)"))
      .addMutator(MutatorFunc(s => s.map(_.solution + 1), "Add1"))
      .addDeletor(DeleteWorstHalfByRandomObjective(numInputs=2))
      .addObjective(Objective[Int](x=> x, "identity", Maximize))
    
    implicit val actorSystem: ActorSystem = ActorSystem("EvvoNode")
    val islandManager = IslandManager.from(10, builder,
      "EvvoNode",
      "src/main/resources/remoting_example.conf")

    islandManager.runBlocking(TerminationCriteria(1.second))

    println(islandManager.currentParetoFrontier())
    actorSystem.terminate()
  }
}
