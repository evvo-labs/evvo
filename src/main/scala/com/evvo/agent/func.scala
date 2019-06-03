package com.evvo.agent

import com.evvo.island.population.Scored
import com.evvo.{CreatorFunctionType, DeletorFunctionType, MutatorFunctionType}


trait CreatorFunction[Sol] {
  /** @return This function's name */
  def name: String

  /** @return The function to call to produce new solutions */
  def create: CreatorFunctionType[Sol]

  def apply(): TraversableOnce[Sol] = create()
}

/**
  * A function that creates a new set of solutions.
  *
  * @param create the function to call to produce new solutions
  * @param name   The name of this creator function.
  */
case class CreatorFunc[Sol](create: CreatorFunctionType[Sol],
                            name: String) extends CreatorFunction[Sol]


trait MutatorFunction[Sol] {
  /**
    * @return This function's name
    */
  def name: String

  /**
    * @return The function to call to produce new solutions
    */
  def mutate: MutatorFunctionType[Sol]

  /** @return The number of solutions to request in the contents of each input set */
  def numInputs: Int

  /**
    * @return Whether this function should run if the number of solutions
    *         in the input is less than `numInputs`
    */
  def shouldRunWithPartialInput: Boolean

  def apply(solutions: IndexedSeq[Scored[Sol]]): TraversableOnce[Sol] = mutate(solutions)
}

/**
  * A function that produces a new set of solutions based on a set of solutions.
  *
  * @param mutate                    Produces a new set of solutions based on a set of solutions.
  * @param name                      The name of the mutator function
  * @param numInputs                 The number of solutions to request in the contents of each
  *                                  input set
  * @param shouldRunWithPartialInput Whether this function should run if the number of solutions
  *                                  in the input is less than `numInputs`
  */
case class MutatorFunc[Sol](mutate: MutatorFunctionType[Sol],
                            name: String,
                            numInputs: Int = 32, // scalastyle:ignore magic.number
                            shouldRunWithPartialInput: Boolean = true)
  extends MutatorFunction[Sol]


trait DeletorFunction[Sol] {
  /** @return This function's name */
  def name: String

  /** @return The function to call to identify which solutions to delete */
  def delete: DeletorFunctionType[Sol]

  /** @return The number of solutions to request in the contents of each input set */
  def numInputs: Int

  /**
    * @return Whether this function should run if the number of solutions
    *         in the input is less than `numInputs`
    */
  def shouldRunWithPartialInput: Boolean

  def apply(solutions: IndexedSeq[Scored[Sol]]): TraversableOnce[Scored[Sol]] = delete(solutions)
}


/**
  * A function that, given a subset of a population, determines which solutions in that subset
  * ought to be deleted.
  *
  * @param delete                    Decides which of a set of solutions to delete.
  * @param name                      The name of the deletor function
  * @param numInputs                 The number of solutions to request in the contents of each
  *                                  input set
  * @param shouldRunWithPartialInput Whether this function should run if the number of solutions
  *                                  in the input is less than `numInputs`
  */
case class DeletorFunc[Sol](delete: DeletorFunctionType[Sol],
                            name: String,
                            numInputs: Int = 32, // scalastyle:ignore magic.number
                            shouldRunWithPartialInput: Boolean = true)
  extends DeletorFunction[Sol]
