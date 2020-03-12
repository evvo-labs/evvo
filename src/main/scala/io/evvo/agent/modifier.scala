package io.evvo.agent

import io.evvo.island.population.Population

import scala.concurrent.duration._

/** An [[io.evvo.agent.Agent]] that grabs some solutions from the population, creates new solutions
  * based on the old ones, and adds the new ones to the population.
  *
  * @param modify The function that generates new solutions from existing ones.
  * @param population The population to put the mutated solutions into.
  * @param strategy How often this agent should run.
  */
class ModifierAgent[Sol](
    modify: ModifierFunction[Sol],
    population: Population[Sol],
    strategy: AgentStrategy = ModifierAgentDefaultStrategy()
) extends AAgent[Sol](modify.name, strategy, population) {

  override protected def step(): Unit = {
    val in = population.getSolutions(modify.numRequestedInputs)
    population.addSolutions(modify(in))
  }

  override def toString: String = s"MutatorAgent[$name]"
}

object ModifierAgent {

  /** @return A new [[io.evvo.agent.ModifierAgent]]. */
  def apply[Sol](
      modifier: ModifierFunction[Sol],
      population: Population[Sol],
      strategy: AgentStrategy = ModifierAgentDefaultStrategy()
  ): ModifierAgent[Sol] =
    new ModifierAgent[Sol](modifier, population, strategy)
}

case class ModifierAgentDefaultStrategy() extends AgentStrategy {
  override def waitTime(populationInformation: PopulationInformation): Duration = {
    0.millis
  }
}
