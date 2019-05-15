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
    if (delete.shouldRunWithPartialInput || in.length == delete.numInputs) {
      val toDelete = delete(in)
      logger.debug(f"Deleted ${toDelete.size} solutions out of ${in.size}")
      population.deleteSolutions(toDelete)
    } else {
      logger.info(s"${this}: not enough solutions in population: " +
        s"got ${in.length}, wanted ${delete.numInputs}")
    }
  }

  override def toString: String = s"DeletorAgent[$name]"
}

case class DeletorAgentDefaultStrategy() extends TAgentStrategy {
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    if (populationInformation.numSolutions < 100) {
      30.millis // give creators a chance!
    } else {
      0.millis
    }
  }
}