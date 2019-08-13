package io.evvo.agent

import akka.event.LoggingAdapter
import io.evvo.island.population.Population

import scala.concurrent.duration._

/** An [[io.evvo.agent.Agent]] that deletes solutions from the population.
  *
  * @param delete A function that, given a set of solutions, tells you which to delete.
  * @param population The population to delete from.
  * @param strategy How often this agent should run.
  */
class DeletorAgent[Sol](
    delete: DeletorFunction[Sol],
    population: Population[Sol],
    strategy: AgentStrategy = DeletorAgentDefaultStrategy()
)(implicit logger: LoggingAdapter)
    extends AAgent[Sol](strategy, population, delete.name)(logger) {

  override protected def step(): Unit = {
    val in = population.getSolutions(delete.numRequestedInputs)
    population.deleteSolutions(delete(in))
  }

  override def toString: String = s"DeletorAgent[$name]"
}

object DeletorAgent {

  /** @return a new [[io.evvo.agent.DeletorAgent]] */
  def apply[Sol](
      delete: DeletorFunction[Sol],
      population: Population[Sol],
      strategy: AgentStrategy = DeletorAgentDefaultStrategy()
  )(implicit logger: LoggingAdapter): DeletorAgent[Sol] =
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
