package com.diatom.agent

import akka.actor.{ActorRef, ActorSystem, Props}
import com.diatom.agent.func.{TCreatorFunc, TMutatorFunc}
import com.diatom.population.TPopulation

trait TMutatorAgent[Sol] extends TAgent[Sol]

case class MutatorAgent[Sol](mutate: TMutatorFunc[Sol], pop: TPopulation[Sol])
  extends AAgent[Sol] with TMutatorAgent[Sol] {

  def step(): Unit = {
    val in = pop.getSolutions(mutate.numInputs)
    val out = mutate.mutate(in)
    pop.addSolutions(out)
  }
}

object MutatorAgent {
  def from[Sol](mutatorFunc: TMutatorFunc[Sol], pop: TPopulation[Sol])
               (implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(MutatorAgent(mutatorFunc, pop)))
  }
}
