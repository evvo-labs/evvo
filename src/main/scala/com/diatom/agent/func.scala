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
  def name: String
}

/**
  * A function that creates a new set of solutions.
  */
trait TCreatorFunc[Sol] extends TAgentFunc with CreatorFunctionType[Sol] {
  val create: CreatorFunctionType[Sol]

  override def apply(): TraversableOnce[Sol] = create()
}

case class CreatorFunc[Sol](create: CreatorFunctionType[Sol],
                            name: String)
  extends TCreatorFunc[Sol] with CreatorFunctionType[Sol] {
}

abstract class InputAcceptingAgentFunc(numInputs: Int = 32,
                                       runWithoutRequestedInputSize: Boolean = true)

/**
  * A function that produces a new set of solutions based on a set of solutions.
  *
  */
trait TMutatorFunc[Sol] extends TAgentFunc with MutatorFunctionType[Sol] {

  /**
    * Produces a new set of solutions based on a set of solutions.
    * param solutions: the solutions to base the new ones off of.
    *
    * @return the new set of solutions.
    */
  val mutate: MutatorFunctionType[Sol]

  /**
    * @return the number of solutions to request in the contents of each set
    */
  val numInputs: Int

  /**
    * @return whether this function should run without the requested input size
    */
  val shouldRunOnPartialInput: Boolean


  override def apply(sample: IndexedSeq[TScored[Sol]]): TraversableOnce[Sol] = mutate(sample)
}

case class MutatorFunc[Sol](mutate: MutatorFunctionType[Sol],
                            name: String,
                            numInputs: Int = 32,
                            shouldRunOnPartialInput: Boolean = true)
  extends TMutatorFunc[Sol]


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
  val delete: DeletorFunctionType[Sol]

  /**
    * @return the number of solutions to request in the contents of each set
    */
  val numInputs: Int

  /**
    * @return whether this function should run without the requested input size
    */
  val shouldRunWithPartialInput: Boolean


  override def apply(sample: IndexedSeq[TScored[Sol]]): TraversableOnce[TScored[Sol]] = {
    delete(sample)
  }
}

case class DeletorFunc[Sol](delete: DeletorFunctionType[Sol],
                            name: String,
                            numInputs: Int = 32,
                            override val shouldRunWithPartialInput: Boolean = true)
  extends TDeletorFunc[Sol]
