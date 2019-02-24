package com.diatom.agent

import com.diatom.agent.func.TMutatorFunc
import com.diatom.population.TPopulation

trait TMutatorAgent[Sol] extends TAgent[Sol] {

}

case class MutatorAgent[Sol](mutate: TMutatorFunc[Sol], pop: TPopulation[Sol]) extends TMutatorAgent[Sol] {

  override def start(): Unit = ???

  override def stop(): Unit = ???

}
