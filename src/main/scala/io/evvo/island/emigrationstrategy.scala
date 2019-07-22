package io.evvo.island

import io.evvo.island.population.{Population, Scored}

import scala.concurrent.duration.{FiniteDuration, _}

/** Determines which solutions in a population are chosen for emigration,
  * and how often emigration happens.
  */
trait EmigrationStrategy {

  /** Determines which solutions in a population are chosen for emigration.
    *
    * @param population The population to choose from.
    * @tparam Sol The type of the solutions in the population and return type. This method, not
    *             the trait, is type-generic, because all implementations of this type should
    *             only operate on the scores, not the solutions themselves.
    * @return The solutions in a population which were chosen for emigration
    */
  def chooseSolutions[Sol](population: Population[Sol]): Seq[Scored[Sol]]

  /** @return How often should emigration happen?
    */
  def durationBetweenRuns: FiniteDuration
}

/** Chooses a random sample of `n` solutions to emigrate */
case class RandomSampleEmigrationStrategy(n: Int, durationBetweenRuns: FiniteDuration = 500.millis)
    extends EmigrationStrategy {
  override def chooseSolutions[Sol](population: Population[Sol]): Seq[Scored[Sol]] = {
    population.getSolutions(n)
  }
}

/** Sends no solutions. */
case object NoEmigrationEmigrationStrategy extends EmigrationStrategy {
  override def chooseSolutions[Sol](population: Population[Sol]): Seq[Scored[Sol]] = Seq.empty

  // It doesn't do anything, no reason to do nothing often.
  override def durationBetweenRuns: FiniteDuration = 100.days
}

/** Chooses the pareto frontier of the population to emigrate. */
case class WholeParetoFrontierEmigrationStrategy(durationBetweenRuns: FiniteDuration = 500.millis)
    extends EmigrationStrategy {
  override def chooseSolutions[Sol](population: Population[Sol]): Seq[Scored[Sol]] = {
    population.getParetoFrontier().solutions.toSeq
  }
}
