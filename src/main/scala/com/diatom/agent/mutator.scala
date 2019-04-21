package com.diatom.agent

import com.diatom.agent.func.TMutatorFunc
import com.diatom.population.TPopulation

import scala.concurrent.duration._

trait TMutatorAgent[Sol] extends TAgent[Sol]

case class MutatorAgent[Sol](mutate: TMutatorFunc[Sol],
                             pop: TPopulation[Sol],
                             strat: TAgentStrategy)
  extends AAgent[Sol](strat, pop) with TMutatorAgent[Sol] {

  def step(): Unit = {
    //TODO validate size of input set
    val in = pop.getSolutions(mutate.numInputs)
    val out = mutate.mutate(in)
    pop.addSolutions(out)
  }
}

object MutatorAgent {
  def from[Sol](mutatorFunc: TMutatorFunc[Sol], pop: TPopulation[Sol])
               : TMutatorAgent[Sol] = {
    val strat = MutatorAgentDefaultStrategy()
    MutatorAgent.from(mutatorFunc, pop, strat)
  }

  def from[Sol](mutatorFunc: TMutatorFunc[Sol], pop: TPopulation[Sol], strat: TAgentStrategy)
               : TMutatorAgent[Sol] = {
    MutatorAgent(mutatorFunc, pop, strat)
  }
}

case class MutatorAgentDefaultStrategy() extends TAgentStrategy {
  // if there are too many solutions, give the deletor chance to work
  // TODO this is bad. fix it.
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    if (populationInformation.numSolutions > 1000) {
      100.millis
    } else {
      0.millis
    }
  }
}
