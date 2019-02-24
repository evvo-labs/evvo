package com.diatom.agent

import com.diatom.population.TPopulation
import com.diatom.agent.func.TCreatorFunc


trait TCreatorAgent[Sol] extends TAgent[Sol] {

}

case class CreatorAgent[Sol](creatorFunc: TCreatorFunc[Sol], pop: TPopulation[Sol])
  extends AAgent[Sol] with TCreatorAgent[Sol] {

  override def step(): Unit = pop.addSolutions(creatorFunc.create())
}
