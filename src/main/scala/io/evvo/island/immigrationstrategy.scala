package io.evvo.island

import io.evvo.island.population.{ParetoFrontier, Population, Scored}

import scala.concurrent.duration._

/** Decides which immigrants should be allowed into an island. */
trait ImmigrationStrategy {

  /**How many immigrants should be added per batch? */
  def numberOfImmigrantsPerBatch: Int

  /** How long to wait between immigration runs? */
  def durationBetweenRuns: Duration

  /** Adds (some of) the immigrants to the population.
    *
    * @param immigrants The set of solutions being sent over to the new population, and their
    *                   scores on each objective.
    * @param population The current population.
    * @return The set of solutions which should be added to the population.
    */
  def addImmigrants[Sol](immigrants: Seq[Scored[Sol]], population: Population[Sol]): Unit
}

/** Only allows new immigrants if they would advance the Pareto frontier on the island. */
case class ElitistImmigrationStrategy(
    override val durationBetweenRuns: Duration = 100.millis,
    override val numberOfImmigrantsPerBatch: Int = 32)
    extends ImmigrationStrategy {
  override def addImmigrants[Sol](
      immigrants: Seq[Scored[Sol]],
      population: Population[Sol]
  ): Unit = {
    val immigrantPareto = ParetoFrontier(immigrants.toSet).solutions
    val currentParetoFrontier = population.getParetoFrontier()
    // Remove the solutions that the pareto frontier dominates, leaving the "Elites".
    population.addScoredSolutions(immigrantPareto.filter(currentParetoFrontier.dominatedBy))
  }
}

/** An immigration strategy that allows all immigrants. */
case class AllowAllImmigrationStrategy(
    override val durationBetweenRuns: Duration = 100.millis,
    override val numberOfImmigrantsPerBatch: Int = 32)
    extends ImmigrationStrategy {
  override def addImmigrants[Sol](
      immigrants: Seq[Scored[Sol]],
      population: Population[Sol]
  ): Unit =
    population.addScoredSolutions(immigrants)
}
