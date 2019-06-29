package com.evvo.island

import com.evvo.island.population.{Population, Scored}

/**
  * Decides which immigrants should be allowed into an island.
  */
trait ImmigrationStrategy {
  /**
    * Decides which of the immigrants to admit into the population.
    * @param immigrants The set of solutions being sent over to the new population, and their
    *                   scores on each objective.
    * @param population The current population.
    * @return The set of solutions which should be added to the population.
    */
  def filter[Sol](immigrants: IndexedSeq[Scored[Sol]], population: Population[Sol])
  : IndexedSeq[Scored[Sol]]
}

/**
  * Only allows new immigrants if they would advance the Pareto frontier on the island.
  */
object ElitistImmigrationStrategy extends ImmigrationStrategy {
  override def filter[Sol](immigrants: IndexedSeq[Scored[Sol]], population: Population[Sol])
  : IndexedSeq[Scored[Sol]] = {
    val currentParetoFrontier = population.getParetoFrontier()
    // Remove the solutions that the pareto frontier dominates, leaving the "Elites".
    immigrants.filter(currentParetoFrontier.dominatedBy)
  }
}

/**
  *
  */
object AllowAllImmigrationStrategy extends ImmigrationStrategy {
  override def filter[Sol](immigrants: IndexedSeq[Scored[Sol]], population: Population[Sol])
  : IndexedSeq[Scored[Sol]] =
    immigrants
}
