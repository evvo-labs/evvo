package com.diatom.population

import com.diatom.agent.{PopulationInformation, TFitnessFunc, TPopulationInformation}
import com.diatom.{ParetoFrontier, Scored, TParetoFrontier, TScored}

import scala.collection.{TraversableOnce, mutable}


// TODO this file is too large, split it into multiple

/**
  * A population is the set of all solutions current in an evolutionary process.
  *
  * @tparam Sol the type of the solutions in the population
  */
trait TPopulation[Sol] {
  // TODO add async calls for non-unit methods, that return a future
  /**
    * Adds the given solutions, if they are unique, subject to any additional constraints
    * imposed on the solutions by the population.
    *
    * @param solutions the solutions to add
    */
  def addSolutions(solutions: TraversableOnce[Sol]): Unit

  /**
    * Selects a random sample of the population.
    *
    * @param n the number of solutions.
    * @return n solutions.
    */
  def getSolutions(n: Int): Set[TScored[Sol]]

  /**
    * Remove the given solutions from the population.
    *
    * @param solutions the solutions to remove
    */
  def deleteSolutions(solutions: TraversableOnce[TScored[Sol]]): Unit

  /**
    * @return the current pareto frontier of this population
    */
  def getParetoFrontier(): TParetoFrontier[Sol]

  /** @return a diagnostic report on this island, for agents to determine how often to run. */
  def getInformation(): TPopulationInformation
}

/**
  * The actor that maintains the population.
  *
  * @tparam Sol the type of the solutions in the population
  */
case class Population[Sol](fitnessFunctionsIter: TraversableOnce[TFitnessFunc[Sol]])
  extends TPopulation[Sol] {

  private val fitnessFunctions = fitnessFunctionsIter.toSet
  private var population = mutable.Set[TScored[Sol]]()
  private var populationVector = Vector[TScored[Sol]]()
  private var getSolutionIndex = 0


  override def addSolutions(solutions: TraversableOnce[Sol]): Unit = {
    //    population = mutable.Set(ParetoFrontier(population.toSet union solutions.map(score).toSet).solutions.toVector:_*)
    population ++= solutions.map(score)
    //    log.debug(f"Current population size ${population.size}")
  }

  private def score(solution: Sol): TScored[Sol] = {
    val scores = fitnessFunctions.map(func => {
      (func.toString, func.score(solution))
    }).toMap
    Scored(scores, solution)
  }


  override def getSolutions(n: Int): Set[TScored[Sol]] = {
    // TODO: This can't be the final impl, inefficient space and time
    if (population.size <= n) {
      population.toSet // no need to randomize, all elements will be included anyway
    } else {
      var out = Set[TScored[Sol]]()
      while (out.size < n) {
        if (!populationVector.isDefinedAt(getSolutionIndex)) {
          populationVector = util.Random.shuffle(population.toVector)
          getSolutionIndex = 0
        }
        out += populationVector(getSolutionIndex)
        getSolutionIndex += 1
      }
      out
    }
  }

  override def deleteSolutions(solutions: TraversableOnce[TScored[Sol]]): Unit = {
    population --= solutions
  }

  override def getParetoFrontier(): TParetoFrontier[Sol] = {
    // TODO test this for performance, and optimize - this is likely to become a bottleneck
    // https://static.aminer.org/pdf/PDF/000/211/201/on_the_computational_complexity_of_finding_the_maxima_of_a.pdf
    ParetoFrontier(this.population.toSet)
  }

  override def getInformation(): TPopulationInformation = {
    val out = PopulationInformation(population.size)
    //    log.debug(s"getInformation returning ${out}")
    out
  }
}
