package io.evvo.agent

import io.evvo.island.population.Scored

/** A named function. Common class for all functions that go in agents, so that utilities and
  * tests can be more abstract.
  *
  * @param name The name of the function
  */
abstract class NamedFunction(val name: String) extends Serializable

/** A function that creates a new set of solutions.
  *
  * @param name the name of this function
  */
abstract class CreatorFunction[Sol](name: String)
    extends NamedFunction(name)
    with (() => Iterable[Sol]) {

  /** @return Produces some new solutions. */
  override final def apply: Iterable[Sol] = create()

  protected def create(): Iterable[Sol]
}

/** A function that derives a new set of solutions from some input set of solutions.
  * See also [[io.evvo.agent.MutatorFunction]] and [[io.evvo.agent.CrossoverFunction]] for
  * convenience classes that allow you to create functions for common tasks.
  *
  * @param name                      This function's name
  * @param numRequestedInputs        The number of solutions to request in the contents of each
  *                                  input set
  * @param shouldRunWithPartialInput Whether this function should run if the number of solutions
  *                                  in the input is less than `numInputs`
  */
abstract class ModifierFunction[Sol](
    name: String,
    val numRequestedInputs: Int = 32,
    val shouldRunWithPartialInput: Boolean = true
) extends NamedFunction(name)
    with (IndexedSeq[Scored[Sol]] => Iterable[Sol]) {

  /** Modifies a sample of the population, producing some new solutions to be added to the
    * population.
    */
  override final def apply(sols: IndexedSeq[Scored[Sol]]): Iterable[Sol] = {
    if (shouldRunWithPartialInput || sols.length == numRequestedInputs) {
      modify(sols)
    } else {
      IndexedSeq.empty
    }
  }

  /** The actual implementation of the modification function. If `shouldRunWithPartialInpunt`
    * is true, this method will never be called on a `sols` which has size different from
    * `numRequestedInputs`.
    */
  protected def modify(sols: IndexedSeq[Scored[Sol]]): Iterable[Sol]
}

/** A function that uses a one-to-one mapping to derive a new set of solutions from some
  * input set of solutions.
  */
abstract class MutatorFunction[Sol](name: String, numInputs: Int = 32)
    extends ModifierFunction[Sol](name, numInputs, shouldRunWithPartialInput = true) {
  // Pass shouldRunWithPartialInput as true always - we know there is no requirement for more
  // than 1 solution at a time, so safe to run on any number of solutions.

  override final def modify(sols: IndexedSeq[Scored[Sol]]): Iterable[Sol] = {
    // We can define modify in terms of mutation, and ask implementors to just provide a one-to-one
    // mutator function to map over the solutions.
    sols.map(_.solution).map(mutate)
  }

  /** The method to apply to create new solutions.
    *
    * @param sol The solution to base the new solution on.
    * @return The new solution, to be added to the population.
    */
  protected def mutate(sol: Sol): Sol
}

/** A function that uses a two-to-one mapping to derive a new set of solutions from some
  * input set of solutions.
  */
abstract class CrossoverFunction[Sol](name: String, numInputs: Int = 32)
    extends ModifierFunction[Sol](name, numInputs, shouldRunWithPartialInput = true) {
  // Pass shouldRunWithPartialInput as true always - we know there is no requirement for more
  // than 2 solutions at a time, so safe to run on, say, 16 or 23 solutions. We will have to be
  // careful about odd numbers of solutions.

  override final def modify(sols: IndexedSeq[Scored[Sol]]): Iterable[Sol] = {
    // We can define modify in terms of crossover, and ask implementors to just provide a two-to-one
    // crossover function to map over the solutions.
    val groups = sols.map(_.solution).grouped(2)

    // Use `collect` and partial functions to ensure that if there is an odd number of solutions,
    // we never hit a runtime error, like we would with indexing into each group.
    groups.collect {
      case IndexedSeq(sol1, sol2) => crossover(sol1, sol2)
    }.toVector
  }

  /** Combines two solutions to produce a new one.
    *
    * @param sol1 One solution. Order is random, and has no meaning.
    * @param sol2 Another solution. Order is random, and has no meaning.
    * @return The new solution, to be added to the population.
    */
  protected def crossover(sol1: Sol, sol2: Sol): Sol
}

/** A function that, given a subset of a population, determines which solutions in that subset
  * ought to be deleted.
  *
  * @param name                      This function's name
  * @param numRequestedInputs        The number of solutions to request in the contents of each
  *                                  input set
  * @param shouldRunWithPartialInput Whether this function should run if the number of solutions
  *                                  in the input is less than `numInputs`
  */
abstract class DeletorFunction[Sol](
    name: String,
    val numRequestedInputs: Int = 32,
    val shouldRunWithPartialInput: Boolean = true
) extends NamedFunction(name)
    with (IndexedSeq[Scored[Sol]] => Iterable[Scored[Sol]]) {

  /** Chooses some solutions from the given Seq to delete. If `shouldRunWithPartialInput` is false,
    * and the number of solutions provided is not the right number, will delete nothing.
    */
  override def apply(sols: IndexedSeq[Scored[Sol]]): Iterable[Scored[Sol]] = {
    if (shouldRunWithPartialInput || sols.length == numRequestedInputs) {
      delete(sols)
    } else {
      IndexedSeq.empty
    }
  }

  /** The actual implementation of the deletion selector. If `shouldRunWithPartialInput`
    * is true, this method will never be called on a `sols` which has size different from
    * `numRequestedInputs`.
    *
    * @return Which solutions from the input Seq should be deleted.
    */
  protected def delete(sols: IndexedSeq[Scored[Sol]]): Iterable[Scored[Sol]]
}
