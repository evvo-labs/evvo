package com.diatom.agent

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.diatom.population.TPopulation
import com.diatom.agent.func.TCreatorFunc


trait TCreatorAgent[Sol] extends TAgent[Sol]

case class CreatorAgent[Sol](creatorFunc: TCreatorFunc[Sol], pop: TPopulation[Sol])
  extends AAgent[Sol] with TCreatorAgent[Sol] with Actor {

  override def step(): Unit = {
    val toAdd = creatorFunc.create()
    pop.addSolutions(toAdd)
  }
}

object CreatorAgent {
  def from[Sol](creatorFunc: TCreatorFunc[Sol], pop: TPopulation[Sol])
               (implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(CreatorAgent(creatorFunc, pop)))
  }
}
