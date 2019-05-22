package com.evvo.remoting

import akka.actor.ActorSystem
import com.evvo.agent
import com.evvo.agent.defaults.DeleteWorstHalfByRandomObjective
import com.evvo.agent.{CreatorFunc, MutatorFunc}
import com.evvo.island.{EvvoIslandBuilder, IslandManager, TerminationCriteria}
import com.evvo.island.population.{Maximize, Objective}

import scala.concurrent.duration._

object Remoting {
  def main(args: Array[String]): Unit = {
    val builder = EvvoIslandBuilder[Int]()
      .addCreator(CreatorFunc(() => Set(0), "Set(0)"))
      .addMutator(MutatorFunc(s => s.map(_.solution + 1), "Add1"))
      .addDeletor(DeleteWorstHalfByRandomObjective(numInputs=2))
      .addObjective(Objective[Int](x=> x, "identity", Maximize))

    implicit val actorSystem: ActorSystem = ActorSystem("RemotingExample")
    val islandManager = IslandManager.from(10, builder,
      "EvvoNode",
      "src/main/resources/remoting_example.conf")

    islandManager.runBlocking(TerminationCriteria(1.second))

    println(islandManager.currentParetoFrontier())
    actorSystem.terminate()
  }
}
