package com.evvo.agent

import com.evvo.island.population.Scored

import scala.collection.TraversableOnce

/**
  * A named function. Common class for all functions that go in agents, so that utilities and
  * tests can be more abstract.
  *
  * @param name The name of the function
  */
abstract class NamedFunction(val name: String) extends Serializable

/**
  * A function that creates a new set of solutions.
  *
  * @param name the name of this function
  */
abstract class CreatorFunction[Sol](name: String) extends NamedFunction(name) {
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
abstract class MutatorFunction[Sol](name: String,
                                    val numInputs: Int = 32,
                                    val shouldRunWithPartialInput: Boolean = true)
  extends NamedFunction(name) {
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
abstract class DeletorFunction[Sol](name: String,
                                    val numInputs: Int = 32,
                                    val shouldRunWithPartialInput: Boolean = true)
  extends NamedFunction(name)  {
  /** @return The function to call to identify which solutions to delete */
  def delete(sols: IndexedSeq[Scored[Sol]]): TraversableOnce[Scored[Sol]]
}
