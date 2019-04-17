package com.diatom.population

import com.diatom.agent.{FitnessFunc, PopulationInformation, TFitnessFunc, TPopulationInformation}
import com.diatom.{FitnessFunctionType, ParetoFrontier, Scored, TParetoFrontier, TScored}

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
    * Returns an array instead of a set for performance to minimize the number of times
    * we call hashcode.
    *
    * @param n the number of unique solutions.
    * @return n unique solutions.
    */
  def getSolutions(n: Int): Array[TScored[Sol]]

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
case class Population[Sol](fitnessFunctionsIter: TraversableOnce[TFitnessFunc[Sol]],
                           hashing: Population.Hashing.Value = Population.Hashing.ON_SCORES)
  extends TPopulation[Sol] {

  private val fitnessFunctions = fitnessFunctionsIter.toSet
  private var population = mutable.Set[TScoredCustomHash]()
  private var populationVector = Vector[TScored[Sol]]()
  private var getSolutionIndex = 0


  override def addSolutions(solutions: TraversableOnce[Sol]): Unit = {
    //    population = mutable.Set(ParetoFrontier(population.toSet union solutions.map(score).toSet).solutions.toVector:_*)
    population ++= solutions.map(score).map(TScoredCustomHash)
    //    log.debug(f"Current population size ${population.size}")
  }

  private def score(solution: Sol): TScored[Sol] = {
    val scores = fitnessFunctions.map(func => {
      (func.toString, func.score(solution))
    }).toMap
    Scored(scores, solution)
  }


  override def getSolutions(n: Int): Array[TScored[Sol]] = {
    // TODO: This can't be the final impl, inefficient space and time
    //    println(s"(population.size, n) = ${(population.size, n)}")
    if (population.size <= n) {
      population.map(_.scored).toArray // no need to randomize, all elements will be included anyway
    } else {
      var out = Array.ofDim[TScored[Sol]](n)
      for (i <- out.indices) {
        if (!populationVector.isDefinedAt(getSolutionIndex)) {
          populationVector = util.Random.shuffle(population.toVector.map(_.scored))
          getSolutionIndex = 0
        }
        out(i) = populationVector(getSolutionIndex)
        getSolutionIndex += 1
      }
      out
    }
  }

  override def deleteSolutions(solutions: TraversableOnce[TScored[Sol]]): Unit = {
    population --= solutions.map(TScoredCustomHash)
  }

  override def getParetoFrontier(): TParetoFrontier[Sol] = {
    // TODO test this for performance, and optimize - this is likely to become a bottleneck
    // https://static.aminer.org/pdf/PDF/000/211/201/on_the_computational_complexity_of_finding_the_maxima_of_a.pdf
    ParetoFrontier(this.population.map(_.scored).toSet)
  }

  override def getInformation(): TPopulationInformation = {
    val out = PopulationInformation(population.size)
    //    log.debug(s"getInformation returning ${out}")
    out
  }


  /**
    * This class is a wrapper for a TScored that changes the hashing behavior so that the
    * set checking for equality can do less work or check for different types of equality.
    *
    * @param scored the tscored
    */
  private case class TScoredCustomHash(scored: TScored[Sol]) {
    override def hashCode(): Int = hashing match {
      case Population.Hashing.ON_SCORES => this.scored.score.hashCode()
      case Population.Hashing.ON_SOLUTIONS => this.scored.solution.hashCode()
    }

    override def equals(obj: Any): Boolean = obj match {
      case that: TScoredCustomHash => hashing match {
        case Population.Hashing.ON_SCORES => this.scored.score.equals(that.scored.score)
        case Population.Hashing.ON_SOLUTIONS => this.scored.solution.equals(that.scored.solution)
      }
      case _ => false
    }
  }
}

object Population {

  object Hashing extends Enumeration {
    type Hashing = Value
    val ON_SOLUTIONS, ON_SCORES = Value
  }
}
