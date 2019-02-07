package com.diatom.agent.func

import com.diatom.TScored

trait TDeletorFunc[Sol] extends TAgentFunc {
  /**
    * Processing a subset of the population, returning some portion of
    * that subset which ought to be deleted.
    * @param solutions a sampling of the population.
    * @return the set of solutions that should be deleted
    */
  def delete(solutions: Set[TScored[Sol]]): Set[Sol]
  /**
    * The size of the set to give to the deletion function.
    * If -1, .
    * @return
    */
  def numInputs: Int
}