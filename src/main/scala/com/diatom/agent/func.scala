package com.diatom.agent

import com.diatom.island.population.TScored
import com.diatom.{CreatorFunctionType, DeletorFunctionType, MutatorFunctionType}

/**
  * A function that an agent can apply repeatedly.
  */
trait TAgentFunc {
  /**
    * @return The name of this function, used entirely for logging purposes.
    */
  def name: String = this.toString
}

/**
  * A function that creates a new set of solutions.
  */
trait TCreatorFunc[Sol] extends TAgentFunc with CreatorFunctionType[Sol] {
  def create: CreatorFunctionType[Sol]

  override def apply(): TraversableOnce[Sol] = create()
}

case class CreatorFunc[Sol](create: CreatorFunctionType[Sol],
                            override val name: String)
  extends TCreatorFunc[Sol] with CreatorFunctionType[Sol] {
}


/**
  * A function that, given a subset of a population, determines which solutions in that subset
  * ought to be deleted.
  */
trait TDeletorFunc[Sol] extends TAgentFunc with DeletorFunctionType[Sol] {
  /**
    * Processing a subset of the population, returning some portion of
    * that subset which ought to be deleted.
    *
    * `@param solutions: a sampling of the population.`
    *
    * @return the set of solutions that should be deleted
    */
  def delete: DeletorFunctionType[Sol]

  /**
    * @return The size of the set to give to the deletion function.
    */
  def numInputs: Int

  override def apply(sample: IndexedSeq[TScored[Sol]]): TraversableOnce[TScored[Sol]] = {
    delete(sample)
  }
}

case class DeletorFunc[Sol](delete: DeletorFunctionType[Sol],
                            override val name: String,
                            numInputs: Int = 64)
  extends TDeletorFunc[Sol]

/**
  * A function that produces a new set of solutions based on a set of solutions.
  */
trait TMutatorFunc[Sol] extends TAgentFunc with MutatorFunctionType[Sol] {

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
  override def apply(sample: IndexedSeq[TScored[Sol]]): TraversableOnce[Sol] = mutate(sample)
}

case class MutatorFunc[Sol](mutate: MutatorFunctionType[Sol],
                            override val name: String,
                            numInputs: Int = 32)
  extends TMutatorFunc[Sol]

