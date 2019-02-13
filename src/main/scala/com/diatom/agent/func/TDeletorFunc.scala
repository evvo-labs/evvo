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

  //  /**
  //    * The size of the set to give to the deletion function.
  //    * If -1, .
  //    * @return
  //    */
  //  def numInputs: Int
}

case class DeletorFunc[Sol](delete: DeletorFunctionType[Sol]) extends TDeletorFunc[Sol]