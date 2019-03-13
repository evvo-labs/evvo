package com.diatom.agent

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import com.diatom.agent.func.{TDeletorFunc, TMutatorFunc}
import com.diatom.population.TPopulation

import scala.concurrent.duration._

trait TDeletorAgent[Sol] extends TAgent[Sol]

case class DeletorAgent[Sol](delete: TDeletorFunc[Sol], pop: TPopulation[Sol], strat: TAgentStrategy)
  extends AAgent[Sol](strat, pop) with TDeletorAgent[Sol] {

  log.info(self.path.toString)

  override protected def step(): Unit = {
    val in = pop.getSolutions(delete.numInputs)
    if (in.size == delete.numInputs) {
      val toDelete = delete.delete(in)
      pop.deleteSolutions(toDelete)
    }
  }
}

object DeletorAgent {

  def from[Sol](deletorFunc: TDeletorFunc[Sol], pop: TPopulation[Sol])
               (implicit system: ActorSystem): ActorRef = {
    val strat: TAgentStrategy = DeletorAgentDefaultStrategy()
    DeletorAgent.from(deletorFunc, pop, strat)
  }

  def from[Sol](deletorFunc: TDeletorFunc[Sol], pop: TPopulation[Sol], strat: TAgentStrategy)
               (implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(DeletorAgent(deletorFunc, pop, strat)), s"DeletorAgent${UUID.randomUUID()}")
  }
}


case class DeletorAgentDefaultStrategy() extends TAgentStrategy {
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    if (populationInformation.numSolutions < 20) {
      // min of 1 and fourth root of num solutions. No particular reason why.
      200.millis // give creators a chance!
    } else if (populationInformation.numSolutions > 300) {
      0.millis
    }
      else
    {
      math.max(1, math.sqrt(math.sqrt(populationInformation.numSolutions))).millis
    }
  }
}