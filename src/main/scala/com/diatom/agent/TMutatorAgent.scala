package com.diatom.agent

import com.diatom.{MutatorFunc, TScored}

trait TMutatorAgent[Sol] extends TAgent[Sol] {
  /**
    * Mutates some solutions, create new solutions which are sent ot the population.
    *
    * @param solutions the solutions to mutate
    * @return the new solutions to be added to the population
    */
  def mutate(solutions: Set[TScored[Sol]]): Set[Sol]
}

case class MutatorAgent[Sol](mutate: MutatorFunc[Sol]) extends TMutatorAgent[Sol] {

  override def start(): Unit = ???

  override def stop(): Unit = ???
}
