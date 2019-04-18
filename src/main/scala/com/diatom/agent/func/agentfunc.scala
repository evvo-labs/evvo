package com.diatom.agent.func

import com.diatom.{CreatorFunctionType, DeletorFunctionType, MutatorFunctionType}

/**
  * A function that an agent can apply repeatedly.
  */
trait TAgentFunc {
  /**
    * @return The unique name.
    */
  def name: String = this.toString
}

/**
  * A function that creates a new set of solutions.
  */
trait TCreatorFunc[Sol] extends TAgentFunc {
  def create: CreatorFunctionType[Sol]
}

case class CreatorFunc[Sol](create: CreatorFunctionType[Sol],
                            override val name: String)
  extends TCreatorFunc[Sol]


/**
  * The function used to determine which solutions to delete.
  */
trait TDeletorFunc[Sol] extends TAgentFunc {
  /**
    * Processing a subset of the population, returning some portion of
    * that subset which ought to be deleted.
    *
    * param solutions: a sampling of the population.
    *
    * @return the set of solutions that should be deleted
    */
  def delete: DeletorFunctionType[Sol]

  /**
    * @return The size of the set to give to the deletion function.
    */
  def numInputs: Int
}

case class DeletorFunc[Sol](delete: DeletorFunctionType[Sol],
                            override val name: String,
                            numInputs: Int = 64)
  extends TDeletorFunc[Sol]

/**
  * A function that produces a new set of solutions based on a set of solutions.
  */
trait TMutatorFunc[Sol] extends TAgentFunc {

  /**
    * Produces a new set of solutions based on a set of solutions.
    * param solutions: the solutions to base the new ones off of.
    *
    * @return the new set of solutions.
    */
  def mutate: MutatorFunctionType[Sol]

  /**
    * @return the number of solutions to pass to mutate.
    */
  def numInputs: Int
}

case class MutatorFunc[Sol](mutate: MutatorFunctionType[Sol],
                            override val name: String,
                            numInputs: Int = 32)
  extends TMutatorFunc[Sol]

