package com.diatom.agent

import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem, Props}
import com.diatom.agent.func.{TCreatorFunc, TMutatorFunc}
import com.diatom.population.TPopulation

trait TMutatorAgent[Sol] extends TAgent[Sol]

case class MutatorAgent[Sol](mutate: TMutatorFunc[Sol],
                             pop: TPopulation[Sol],
                             strat: TAgentStrategy)
  extends AAgent[Sol](strat, pop) with TMutatorAgent[Sol] {

  def step(): Unit = {
    val in = pop.getSolutions(mutate.numInputs)
    val out = mutate.mutate(in)
    pop.addSolutions(out)
  }
}

object MutatorAgent {
  def from[Sol](mutatorFunc: TMutatorFunc[Sol], pop: TPopulation[Sol])
               (implicit system: ActorSystem): ActorRef = {
    val strat = MutatorAgentDefaultStrategy()
    MutatorAgent.from(mutatorFunc, pop, strat)
  }

  def from[Sol](mutatorFunc: TMutatorFunc[Sol], pop: TPopulation[Sol], strat: TAgentStrategy)
               (implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(MutatorAgent(mutatorFunc, pop, strat)))
  }
}

case class MutatorAgentDefaultStrategy() extends TAgentStrategy {
  // if there are too many solutions, give the deletor chance to work
  // this is bad. fix it.
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    if (populationInformation.numSolutions > 1000) {
      100.millis
    } else {
      1.millis
    }
  }
}
