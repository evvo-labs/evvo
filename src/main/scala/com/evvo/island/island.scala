package com.evvo.island

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import com.evvo.island.population.ParetoFrontier

/**
  * `EvolutionaryProcess` is a generic interface for evolutionary problem solvers.
  */
trait EvolutionaryProcess[Solution] {
  /**
    * Run this island, until the specified termination criteria is met. This call will block
    * until the termination criteria is completed.
    *
    * @return this
    */
  def runBlocking(stopAfter: StopAfter): Unit

  def runAsync(stopAfter: StopAfter): Future[Unit]

  /**
    * @return the current pareto frontier of solutions on this island?
    */
  def currentParetoFrontier(): ParetoFrontier[Solution]

  /**
    * Provides a set of solutions to be added to the population of an EvolutionaryProcess.
    *
    * @param solutions the solutions to add
    */
  def emigrate(solutions: Seq[Solution])

  /**
    * Sends a poison pill to the evolutionary process.
    */
  def poisonPill(): Unit
}

/**
  * Defines how long an evolutionary process should be run for.
  *
  * @param time Specifies a maximum duration to run, for example, `1.second` will stop running
  *             the evolutionary process after one second.
  */
case class StopAfter(time: Duration)
