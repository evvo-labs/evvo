package com.evvo.agent

import akka.event.LoggingAdapter
import com.evvo.island.population.Population
import scala.concurrent.duration._

case class DeletorAgent[Sol](delete: DeletorFunction[Sol],
                             population: Population[Sol],
                             strategy: AgentStrategy = DeletorAgentDefaultStrategy())
                            (implicit val logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, delete.name)(logger) {

  override protected def step(): Unit = {
    val in = population.getSolutions(delete.numInputs)
    if (delete.shouldRunWithPartialInput || in.length == delete.numInputs) {
      val toDelete = delete.delete(in)
      logger.debug(f"Deleted ${toDelete.size} solutions out of ${in.size}")
      population.deleteSolutions(toDelete)
    } else {
      logger.info(s"${this}: not enough solutions in population: " +
        s"got ${in.length}, wanted ${delete.numInputs}")
    }
  }

  override def toString: String = s"DeletorAgent[$name]"
}

case class DeletorAgentDefaultStrategy() extends AgentStrategy {
  override def waitTime(populationInformation: PopulationInformation): Duration = {
    if (populationInformation.numSolutions < 100) {
      30.millis // give creators a chance!
    } else {
      0.millis
    }
  }
}
