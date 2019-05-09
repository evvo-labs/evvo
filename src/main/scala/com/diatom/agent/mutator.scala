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


  def step(): Unit = {
    val in = population.getSolutions(mutate.numInputs)
    if (mutate.shouldRunOnPartialInput || in.length == mutate.numInputs) {
      val mutatedSolutions = mutate(in)
      population.addSolutions(mutatedSolutions)
    } else {
      logger.info(s"${this}: not enough solutions in population: " +
        s"got ${in.length}, wanted ${mutate.numInputs}")
    }
  }

  override def toString: String =  s"MutatorAgent[$name, $numInvocations]"

}

case class MutatorAgentDefaultStrategy() extends TAgentStrategy {
  // if there are too many solutions, give the deletor chance to work
  // TODO this is bad. fix it.
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    if (populationInformation.numSolutions > 1000) {
      100.millis
    } else {
      0.millis
    }
  }
}
