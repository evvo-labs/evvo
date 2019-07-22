package io.evvo.island

import io.evvo.island.population.Population

import scala.concurrent.duration._

/**
  * A strategy that determines what is logged, and when.
  */
trait LoggingStrategy {

  /** How long to wait between runs of the logger. */
  def durationBetweenLogs: FiniteDuration

  /** Logs the current population.
    *
    * @param population The population to log, passed from the island.
    * @return The string that should be logged based on the population.
    */
  def logPopulation(population: Population[_]): String
}

case class LogPopulation(durationBetweenLogs: FiniteDuration = 1.second) extends LoggingStrategy {
  override def logPopulation(population: Population[_]): String = {
    f"""
     |Size: ${population.getInformation().numSolutions}
     |Pareto Frontier: ${population.getParetoFrontier()}
     """.stripMargin
  }
}
