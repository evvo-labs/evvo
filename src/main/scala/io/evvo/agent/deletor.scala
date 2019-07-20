package io.evvo.agent

import akka.event.LoggingAdapter
import io.evvo.island.population.Population

import scala.concurrent.duration._

/** An [[io.evvo.agent.Agent]] that deletes solutions from the population.
  *
  * @param delete A function that, given a set of solutions, tells you which to delete.
  */
class DeletorAgent[Sol](delete: DeletorFunction[Sol],
                        population: Population[Sol],
                        strategy: AgentStrategy = DeletorAgentDefaultStrategy())
                       (implicit logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, delete.name)(logger) {

  override protected def step(): Unit = {
    val in = population.getSolutions(delete.numInputs)
    if (delete.shouldRunWithPartialInput || in.length == delete.numInputs) {
      val toDelete = delete.delete(in)
      logger.debug(f"Deleted ${toDelete.iterator.size} solutions out of ${in.size}")
      population.deleteSolutions(toDelete)
    } else {
      logger.debug(s"${this}: not enough solutions in population: " +
        s"got ${in.length}, wanted ${delete.numInputs}")
    }
  }

  override def toString: String = s"DeletorAgent[$name]"
}

object DeletorAgent {
  /** @return a new [[io.evvo.agent.DeletorAgent]] */
  def apply[Sol](delete: DeletorFunction[Sol],
                 population: Population[Sol],
                 strategy: AgentStrategy = DeletorAgentDefaultStrategy())
                (implicit logger: LoggingAdapter): DeletorAgent[Sol] =
    new DeletorAgent(delete, population, strategy)
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
