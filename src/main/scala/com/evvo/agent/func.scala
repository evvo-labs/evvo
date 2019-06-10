package com.evvo.agent

import com.evvo.island.population.Scored
import com.evvo.utils.UntypedSerializationPackage
import com.evvo.{CreatorFunctionType, DeletorFunctionType, MutatorFunctionType}

import scala.collection.TraversableOnce

/**
  * A function that creates a new set of solutions.
  */
abstract class CreatorFunction[Sol] extends Serializable {
  /** @return This function's name */
  def name: String

  /** @return The function to call to produce new solutions */
  def create(): TraversableOnce[Sol]
}

/**
  * A function that produces a new set of solutions based on a set of solutions.
  */
abstract class MutatorFunction[Sol] extends Serializable {
  /**
    * @return This function's name
    */
  def name: String

  /**
    * @return The function to call to produce new solutions
    */
  def mutate(sols: IndexedSeq[Scored[Sol]]): TraversableOnce[Sol]

  /** @return The number of solutions to request in the contents of each input set */
  def numInputs: Int = 32 // scalastyle:ignore magic.number

  /**
    * @return Whether this function should run if the number of solutions
    *         in the input is less than `numInputs`
    */
  def shouldRunWithPartialInput: Boolean = true
}

/**
  * A function that, given a subset of a population, determines which solutions in that subset
  * ought to be deleted.
  */
abstract class DeletorFunction[Sol] extends Serializable {
  /** @return This function's name */
  def name: String

  /** @return The function to call to identify which solutions to delete */
  def delete(sols: IndexedSeq[Scored[Sol]]): TraversableOnce[Scored[Sol]]

  /** @return The number of solutions to request in the contents of each input set */
  def numInputs: Int = 32 // scalastyle:ignore magic.number

  /**
    * @return Whether this function should run if the number of solutions
    *         in the input is less than `numInputs`
    */
  def shouldRunWithPartialInput: Boolean = true
}
