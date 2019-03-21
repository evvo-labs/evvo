package com.diatom.agent.func

import com.diatom.DeletorFunctionType

/**
  * The function used to determine which solutions to delete.
  */
trait TDeletorFunc[Sol] extends TAgentFunc {
  /**
    * Processing a subset of the population, returning some portion of
    * that subset which ought to be deleted.
    *
    * param solutions: a sampling of the population.
    * @return the set of solutions that should be deleted
    */
  def delete: DeletorFunctionType[Sol]

    /**
      * @return The size of the set to give to the deletion function.
      */
    def numInputs: Int = 64
}

case class DeletorFunc[Sol](delete: DeletorFunctionType[Sol]) extends TDeletorFunc[Sol]