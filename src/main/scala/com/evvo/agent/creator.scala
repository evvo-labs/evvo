package com.evvo.agent

import akka.event.LoggingAdapter
import com.evvo.island.population.Population
import scala.concurrent.duration._


case class CreatorAgent[Sol](create: CreatorFunction[Sol],
                             population: Population[Sol],
                             strategy: AgentStrategy = CreatorAgentDefaultStrategy())
                            (implicit val logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, create.name)(logger) {

  override protected def step(): Unit = {
    logger.info(s"Step started for $this")
    val toAdd = create.create()
    logger.info(s"Created solutions, $toAdd , $this")
    population.addSolutions(toAdd)
    logger.info(s"Step over")
  }

  override def toString: String = s"CreatorAgent[$name]"
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
