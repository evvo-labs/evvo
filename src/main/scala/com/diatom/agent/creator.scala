package com.diatom.agent

import com.diatom.TPopulation
import com.diatom.agent.func.TCreatorFunc
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._


trait TCreatorAgent[Sol] extends TAgent[Sol]

case class CreatorAgent[Sol](create: TCreatorFunc[Sol],
                             pop: TPopulation[Sol],
                             strat: TAgentStrategy = CreatorAgentDefaultStrategy())
  extends AAgent[Sol](strat, pop, create.name) with TCreatorAgent[Sol] {

  override def step(): Unit = {
    val toAdd = create.create()
    log.debug(s"created ${toAdd}")
    pop.addSolutions(toAdd)
  }
}

case class CreatorAgentDefaultStrategy() extends TAgentStrategy {
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    val n = populationInformation.numSolutions

    if (n > 1000) {
      1000.millis // they have 1000 solutions, they're probably set.
    } else {
      math.max(1, math.exp(.001 * n)).millis
    }
  }
}
