package com.diatom.agent

import com.diatom.agent.func.TDeletorFunc
import com.diatom.population.TPopulation

trait TDeletorAgent[Sol] extends TAgent[Sol] {

}

case class DeletorAgent[Sol](delete: TDeletorFunc[Sol], pop: TPopulation[Sol])
  extends AAgent[Sol] with TDeletorAgent[Sol] {

  override protected def step(): Unit = {
    val in = pop.getSolutions(delete.numInputs)
    val toDelete = delete.delete(in)
    pop.deleteSolutions(toDelete)
  }
}

