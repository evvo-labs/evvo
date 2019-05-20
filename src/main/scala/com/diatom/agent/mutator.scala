package com.diatom.agent

import akka.event.LoggingAdapter
import com.diatom.island.population.TPopulation

import scala.concurrent.duration._

trait TMutatorAgent[Sol] extends TAgent[Sol]

case class MutatorAgent[Sol](mutate: TMutatorFunc[Sol],
                             population: TPopulation[Sol],
                             strategy: TAgentStrategy = MutatorAgentDefaultStrategy())
                            (implicit val logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, mutate.name)(logger) with TMutatorAgent[Sol] {

  override protected def step(): Unit = {
    val in = population.getSolutions(mutate.numInputs)
    if (mutate.shouldRunOnPartialInput || in.length == mutate.numInputs) {
      val mutatedSolutions = mutate(in)
      population.addSolutions(mutatedSolutions)
    } else {
      logger.info(s"${this}: not enough solutions in population: " +
        s"got ${in.length}, wanted ${mutate.numInputs}")
    }
  }

  override def toString: String = s"MutatorAgent[$name, $numInvocations]"

}

case class MutatorAgentDefaultStrategy() extends TAgentStrategy {
  // TODO this is bad. fix it.
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    0.millis
  }
}
