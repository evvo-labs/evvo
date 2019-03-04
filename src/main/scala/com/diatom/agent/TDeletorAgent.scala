package com.diatom.agent

import akka.actor.{ActorRef, ActorSystem, Props}
import com.diatom.agent.func.{TDeletorFunc, TMutatorFunc}
import com.diatom.population.TPopulation

trait TDeletorAgent[Sol] extends TAgent[Sol]

case class DeletorAgent[Sol](delete: TDeletorFunc[Sol], pop: TPopulation[Sol])
  extends AAgent[Sol] with TDeletorAgent[Sol] {

  override protected def step(): Unit = {
    val in = pop.getSolutions(delete.numInputs)
    val toDelete = delete.delete(in)
    pop.deleteSolutions(toDelete)
  }
}

object DeletorAgent {
  def from[Sol](deletorFunc: TDeletorFunc[Sol], pop: TPopulation[Sol])
               (implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(DeletorAgent(deletorFunc, pop)))
  }
}


