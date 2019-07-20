package io.evvo.agent

import akka.event.LoggingAdapter
import io.evvo.island.population.Population

import scala.concurrent.duration._

/** An [[io.evvo.agent.Agent]] that grabs some solutions from the population, creates new solutions
  * based on the old ones, and adds the new ones to the population.
  *
  * @param modifier The function that generates new solutions from existing ones.
  */
class ModifierAgent[Sol](modifier: ModifierFunction[Sol],
                         population: Population[Sol],
                         strategy: AgentStrategy = ModifierAgentDefaultStrategy())
                        (implicit logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, modifier.name)(logger) {

  override protected def step(): Unit = {
    val in = population.getSolutions(modifier.numInputs)
    if (modifier.shouldRunWithPartialInput || in.length == modifier.numInputs) {
      val newSolutions = modifier.modify(in)
      population.addSolutions(newSolutions)
    } else {
      logger.debug(s"${this}: not enough solutions in population: " +
        s"got ${in.length}, wanted ${modifier.numInputs}")
    }
  }

  override def toString: String = s"MutatorAgent[$name]"
}

object ModifierAgent {
  /** @return A new [[io.evvo.agent.ModifierAgent]]. */
  def apply[Sol](modifier: ModifierFunction[Sol],
                 population: Population[Sol],
                 strategy: AgentStrategy = ModifierAgentDefaultStrategy())
                (implicit logger: LoggingAdapter): ModifierAgent[Sol] =
    new ModifierAgent[Sol](modifier, population, strategy)
}

case class ModifierAgentDefaultStrategy() extends AgentStrategy {
  override def waitTime(populationInformation: PopulationInformation): Duration = {
    0.millis
  }
}
