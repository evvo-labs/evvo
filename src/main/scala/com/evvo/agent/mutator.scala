package com.evvo.agent

import akka.event.LoggingAdapter
import com.evvo.island.population.Population

import scala.concurrent.duration._


case class MutatorAgent[Sol](mutate: MutatorFunction[Sol],
                             population: Population[Sol],
                             strategy: AgentStrategy = MutatorAgentDefaultStrategy())
                            (implicit val logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, mutate.name)(logger) {

  override protected def step(): Unit = {
    val in = population.getSolutions(mutate.numInputs)
    if (mutate.shouldRunWithPartialInput || in.length == mutate.numInputs) {
      val mutatedSolutions = mutate(in)
      population.addSolutions(mutatedSolutions)
    } else {
      logger.info(s"${this}: not enough solutions in population: " +
        s"got ${in.length}, wanted ${mutate.numInputs}")
    }
  }

  override def toString: String = s"MutatorAgent[$name, $numInvocations]"
}

case class MutatorAgentDefaultStrategy() extends AgentStrategy {
  // TODO this is bad. fix it.
  override def waitTime(populationInformation: PopulationInformation): Duration = {
    0.millis
  }
}
