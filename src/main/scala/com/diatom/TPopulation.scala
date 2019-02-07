package com.diatom

/**
  * A population is the set of all solutions current in an evolutionary process.
  * @tparam Solution the type of the solutions in the population
  */
trait TPopulation[Solution] {
  /**
    * Adds the given solutions, if they are unique, subject to any additional constraints
    * imposed on the solutions by the population.
    * @param solutions the solutions to add
    */
  def addSolutions(solutions: TraversableOnce[Solution]): Unit
}
