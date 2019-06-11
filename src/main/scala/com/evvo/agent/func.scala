package com.evvo.agent

import com.evvo.island.population.Scored

import scala.collection.TraversableOnce

/**
  * A function that creates a new set of solutions.
  *
  * @param name the name of this function
  */
abstract class CreatorFunction[Sol](val name: String) extends Serializable {

  /** @return The function to call to produce new solutions */
  def create(): TraversableOnce[Sol]
}

/**
  * A function that produces a new set of solutions based on a set of solutions.
  *
  * @param name                      This function's name
  * @param numInputs                 The number of solutions to request in the contents of each
  *                                  input set
  * @param shouldRunWithPartialInput Whether this function should run if the number of solutions
  *                                  in the input is less than `numInputs`
  */
abstract class MutatorFunction[Sol](val name: String,
                                    val numInputs: Int = 32,
                                    val shouldRunWithPartialInput: Boolean = true)
  extends Serializable {
  /**
    * @return The function to call to produce new solutions
    */
  def mutate(sols: IndexedSeq[Scored[Sol]]): TraversableOnce[Sol]
}

/**
  * A function that, given a subset of a population, determines which solutions in that subset
  * ought to be deleted.
  *
  * @param name                      This function's name
  * @param numInputs                 The number of solutions to request in the contents of each
  *                                  input set
  * @param shouldRunWithPartialInput Whether this function should run if the number of solutions
  *                                  in the input is less than `numInputs`
  */
abstract class DeletorFunction[Sol](val name: String,
                                    val numInputs: Int = 32,
                                    val shouldRunWithPartialInput: Boolean = true)
  extends Serializable {
  /** @return The function to call to identify which solutions to delete */
  def delete(sols: IndexedSeq[Scored[Sol]]): TraversableOnce[Scored[Sol]]
}
