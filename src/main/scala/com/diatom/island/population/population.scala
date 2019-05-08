package com.diatom.island.population

import com.diatom.agent.{PopulationInformation, TPopulationInformation}
import org.slf4j.LoggerFactory

import scala.collection.{TraversableOnce, mutable}


/**
  * A population is the set of all solutions current in an evolutionary process.
  *
  * @tparam Sol the type of the solutions in the population
  */
trait TPopulation[Sol] {
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
    * Returns an array instead of a set for performance to minimize the number of times
    * we call hashcode.
    *
    * @param n the number of unique solutions.
    * @return n unique solutions.
    */
  def getSolutions(n: Int): IndexedSeq[TScored[Sol]]

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
case class Population[Sol](fitnessFunctionsIter: TraversableOnce[TObjective[Sol]],
                           hashing: HashingStrategy.Value = HashingStrategy.ON_SCORES)
  extends TPopulation[Sol] {

  private val log = LoggerFactory.getLogger(this.getClass)

  private val fitnessFunctions = fitnessFunctionsIter.toSet
  private var population = mutable.Set[TScored[Sol]]()
  private var populationVector = Vector[TScored[Sol]]()
  private var getSolutionIndex = 0


  override def addSolutions(solutions: TraversableOnce[Sol]): Unit = {
    population ++= solutions.map(score)
//    log.debug(f"Added ${solutions.size} solutions, new population size ${population.size}")
  }

  private def score(solution: Sol): TScored[Sol] = {
    val scores = fitnessFunctions.map(func => {
      (func.toString, func.optimizationDirection) -> func.score(solution)
    }).toMap
    Scored(scores, solution, hashing)
  }


  override def getSolutions(n: Int): Vector[TScored[Sol]] = {
    // TODO: This can't be the final impl, inefficient space and time
//    if (population.size <= n) {
//      population.toVector // no need to randomize, all elements will be included anyway
//    } else {
//
//      var out = Array.ofDim[TScored[Sol]](n)
//      for (i <- out.indices) {
//        if (!populationVector.isDefinedAt(getSolutionIndex)) {
//          populationVector = util.Random.shuffle(population.toVector)
//          getSolutionIndex = 0
//        }
//        out(i) = populationVector(getSolutionIndex)
//        getSolutionIndex += 1
//      }
      util.Random.shuffle(population.toVector).take(n)
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
    log.debug(s"getInformation returning ${out}")
    out
  }
}

