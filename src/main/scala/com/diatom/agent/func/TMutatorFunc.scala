package com.diatom.agent.func

import com.diatom.MutatorFunctionType

/**
  * A function that produces a new set of solutions based on a set of solutions.
  */
trait TMutatorFunc[Sol] extends TAgentFunc {

  /**
    * Produces a new set of solutions based on a set of solutions.
    * param solutions: the solutions to base the new ones off of.
    * @return the new set of solutions.
    */
  def mutate: MutatorFunctionType[Sol]

  /**
    * @return the number of solutions to pass to mutate.
    */
  def numInputs: Int = 32
}

case class MutatorFunc[Sol](mutate: MutatorFunctionType[Sol]) extends TMutatorFunc[Sol]

