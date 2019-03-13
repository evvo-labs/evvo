package com.diatom.agent

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.diatom.population.TPopulation
import com.diatom.agent.func.TCreatorFunc

import scala.concurrent.duration._


trait TCreatorAgent[Sol] extends TAgent[Sol]

case class CreatorAgent[Sol](creatorFunc: TCreatorFunc[Sol],
                             pop: TPopulation[Sol],
                             strat: TAgentStrategy)
  extends AAgent[Sol](strat, pop) with TCreatorAgent[Sol] with Actor {

  override def step(): Unit = {
    val toAdd = creatorFunc.create()
    pop.addSolutions(toAdd)
  }
}


object CreatorAgent {
  def from[Sol](creatorFunc: TCreatorFunc[Sol], pop: TPopulation[Sol])
               (implicit system: ActorSystem): ActorRef = {
    val strat: TAgentStrategy = _ => 50.millis // TODO replace with sane default
    CreatorAgent.from(creatorFunc, pop, strat)
  }

  def from[Sol](creatorFunc: TCreatorFunc[Sol], pop: TPopulation[Sol], strat: TAgentStrategy)
               (implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(CreatorAgent(creatorFunc, pop, strat)), s"CreatorAgent${UUID.randomUUID()}")
  }
}

case class CreatorAgentDefaultStrategy() extends TAgentStrategy {
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    val n = populationInformation.numSolutions

    if (n > 1000) {
      1000.millis // they have 1000 solutions, they're probably set.
    } else {
      math.max(1, math.exp(.001 * n)).millis
    }
  }
}
