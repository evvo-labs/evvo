package com.diatom.agent

import akka.event.LoggingAdapter
import com.diatom.island.population.TPopulation
import scala.concurrent.duration._


trait TCreatorAgent[Sol] extends TAgent[Sol]

case class CreatorAgent[Sol](create: TCreatorFunc[Sol],
                             population: TPopulation[Sol],
                             strategy: TAgentStrategy = CreatorAgentDefaultStrategy())
                            (implicit val logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, create.name)(logger) with TCreatorAgent[Sol] {

  override protected def step(): Unit = {
    val toAdd = create.create()
    population.addSolutions(toAdd)
  }

  override def toString: String = s"CreatorAgent[$name]"
}

case class CreatorAgentDefaultStrategy() extends TAgentStrategy {
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    val n = populationInformation.numSolutions
    if (n > 300) {
      1000.millis // they have 1000 solutions, they're probably set.
    } else {
      0.millis
    }
  }
}
