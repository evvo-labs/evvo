package com.diatom.agent

import akka.event.LoggingAdapter
import com.diatom.island.population.TPopulation
import scala.concurrent.duration._

trait TDeletorAgent[Sol] extends TAgent[Sol]

case class DeletorAgent[Sol](delete: TDeletorFunc[Sol],
                             population: TPopulation[Sol],
                             strategy: TAgentStrategy = DeletorAgentDefaultStrategy())
                            (implicit val logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, delete.name)(logger) with TDeletorAgent[Sol] {

  override protected def step(): Unit = {
    val in = population.getSolutions(delete.numInputs)
    // TODO configure whether to allow running without
    if (delete.shouldRunWithPartialInput || in.length == delete.numInputs) {
      val toDelete = delete(in)
      population.deleteSolutions(toDelete)
    } else {
      logger.info(s"${this}: not enough solutions in population: " +
        s"got ${in.length}, wanted ${delete.numInputs}")
    }
  }

  override def toString: String = s"Agent[$name, $numInvocations]"
}

case class DeletorAgentDefaultStrategy() extends TAgentStrategy {
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    if (populationInformation.numSolutions < 20) {
      30.millis // give creators a chance!
    } else if (populationInformation.numSolutions > 300) {
      0.millis
    }
    else {
      // min of 1 and fourth root of num solutions. No particular reason why.
      math.max(1, math.sqrt(math.sqrt(populationInformation.numSolutions))).millis
    }
  }
}