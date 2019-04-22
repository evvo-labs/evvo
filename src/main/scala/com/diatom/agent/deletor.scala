package com.diatom.agent

import com.diatom.TPopulation
import com.diatom.agent.func.TDeletorFunc
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._

trait TDeletorAgent[Sol] extends TAgent[Sol]

case class DeletorAgent[Sol](delete: TDeletorFunc[Sol],
                             pop: TPopulation[Sol],
                             strat: TAgentStrategy)
  extends AAgent[Sol](strat, pop, delete.name) with TDeletorAgent[Sol] {

  override protected def step(): Unit = {
    val in = pop.getSolutions(delete.numInputs)
    if (in.length == delete.numInputs) {
      val toDelete = delete.delete(in)
      log.debug(s"deleting ${toDelete} out of ${in}")
      pop.deleteSolutions(toDelete)
    } else {
      log.warn(s"not enough solutions in population: got ${in.length}, wanted ${delete.numInputs}")
    }
  }
}

object DeletorAgent {
  def from[Sol](deletorFunc: TDeletorFunc[Sol],
                pop: TPopulation[Sol],
                strat: TAgentStrategy = DeletorAgentDefaultStrategy())
               (implicit log: Logger)
  : TDeletorAgent[Sol] = {
    DeletorAgent(deletorFunc, pop, strat)
  }
}


case class DeletorAgentDefaultStrategy() extends TAgentStrategy {
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    if (populationInformation.numSolutions < 20) {
      30.millis // give creators a chance!
    } else if (populationInformation.numSolutions > 300) {
      0.millis
    }
    else {
      // min of 1 and fourth root of num solutions. No particular reason why.
      math.max(1, math.sqrt(math.sqrt(populationInformation.numSolutions))).millis
    }
  }
}