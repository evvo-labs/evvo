package io.evvo.agent

import akka.event.LoggingAdapter
import io.evvo.island.population.Population

import scala.concurrent.duration._


/** An [[io.evvo.agent.Agent]] that produces new solutions and adds them to the population.
  *
  * @param create The function that creates new solutions.
  */
class CreatorAgent[Sol](create: CreatorFunction[Sol],
                        population: Population[Sol],
                        strategy: AgentStrategy = CreatorAgentDefaultStrategy())
                       (implicit logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, create.name)(logger) {
  override protected def step(): Unit = {
    val toAdd = create.create()
    logger.debug(s"Created solutions, $toAdd , $this")
    population.addSolutions(toAdd)
  }

  override def toString: String = s"CreatorAgent[$name]"
}

object CreatorAgent {
  /** @return A new [[io.evvo.agent.CreatorAgent]]. */
  def apply[Sol](create: CreatorFunction[Sol],
                 population: Population[Sol],
                 strategy: AgentStrategy = CreatorAgentDefaultStrategy())
                (implicit logger: LoggingAdapter): CreatorAgent[Sol] =
    new CreatorAgent(create, population, strategy)
}

case class CreatorAgentDefaultStrategy() extends AgentStrategy {
  override def waitTime(populationInformation: PopulationInformation): Duration = {
    val n = populationInformation.numSolutions
    if (n > 300) {
      1000.millis // they have 1000 solutions, they're probably set.
    } else {
      0.millis
    }
  }
}
