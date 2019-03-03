package com.diatom.agent

import com.diatom.agent.func.TMutatorFunc
import com.diatom.population.TPopulation

trait TMutatorAgent[Sol] extends TAgent[Sol] {

}

case class MutatorAgent[Sol](mutate: TMutatorFunc[Sol], pop: TPopulation[Sol])
  extends AAgent[Sol] with TMutatorAgent[Sol] {

  def step(): Unit = {
    val in = pop.getSolutions(mutate.numInputs)
    val out = mutate.mutate(in)
    pop.addSolutions(out)
  }
}
