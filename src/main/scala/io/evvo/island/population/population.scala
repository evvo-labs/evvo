package io.evvo.island.population

import akka.event.LoggingAdapter
import io.evvo.agent.PopulationInformation

import scala.collection.parallel.mutable


/**
  * A population is the set of all solutions current in an evolutionary process. Currently,
  * the only extending class is StandardPopulation, but other classes may have different behavior,
  * such as only ever keeping the non-dominated set, or only keeping points that are dominated
  * by <=2 points, etc.
  *
  * @tparam Sol the type of the solutions in the population
  */
trait Population[Sol] {
  /**
    * Adds the given solutions, if they are unique, subject to any additional constraints
    * imposed on the solutions by the population.
    *
    * @param solutions the solutions to add
    */
  def addSolutions(solutions: Iterable[Sol]): Unit

  /**
    * Selects a random sample of the population.
    *
    * Returns an array instead of a set for performance to minimize the number of times
    * we call hashcode.
    *
    * @param n the number of unique solutions.
    * @return n unique solutions.
    */
  def getSolutions(n: Int): IndexedSeq[Scored[Sol]]

  /**
    * Remove the given solutions from the population.
    *
    * @param solutions the solutions to remove
    */
  def deleteSolutions(solutions: Iterable[Scored[Sol]]): Unit

  /**
    * @return the current pareto frontier of this population
    */
  def getParetoFrontier(): ParetoFrontier[Sol]

  /** @return a diagnostic report on this island, for agents to determine how often to run. */
  def getInformation(): PopulationInformation
}


/**
  * A population, as a set of scored solutions. Will contain no duplicates by score, unless
  * hashing is `HashingStrategy.ON_SOLUTIONS`.
  *
  * @tparam Sol the type of the solutions in the population
  */
case class StandardPopulation[Sol](objectivesIter: Iterable[Objective[Sol]],
                                   hashing: HashingStrategy.Value = HashingStrategy.ON_SCORES)
                                  (implicit val logger: LoggingAdapter)
  extends Population[Sol] {
  private val objectives = objectivesIter.iterator.toSet
  private var population = Set[Scored[Sol]]()

  override def addSolutions(solutions: Iterable[Sol]): Unit = {
    population ++= solutions.iterator.map(score)
    logger.debug(f"Added ${solutions.iterator.size} solutions, new population size ${population.size}")
  }

  private def score(solution: Sol): Scored[Sol] = {
    val scores = this.objectives.map(func =>
      (func.name, func.optimizationDirection) -> func.score(solution)
    ).toMap
    val out = Scored(scores, solution, hashing)
    logger.debug(s"StandardPopulation: created $out")
    out
  }

  override def getSolutions(n: Int): IndexedSeq[Scored[Sol]] = {
    util.Random.shuffle(this.population.toVector).take(n)
  }

  override def deleteSolutions(solutions: Iterable[Scored[Sol]]): Unit = {
    this.population --= solutions
  }

  override def getParetoFrontier(): ParetoFrontier[Sol] = {
    ParetoFrontier(this.population)
  }

  override def getInformation(): PopulationInformation = {
    val out = PopulationInformation(this.population.size)
    logger.debug(s"StandardPopulation: getInformation returning ${out}")
    out
  }
}

