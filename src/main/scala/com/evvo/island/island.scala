package com.evvo.island

import com.evvo.island.population.TParetoFrontier

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * An evolutionary process with a set of actors that all act upon one set of solutions.
  */
trait TEvolutionaryProcess[Solution] {
  /**
    * Run this island, until the specified termination criteria is met. This call will block
    * until the termination criteria is completed.
    *
    * @return this
    */
  def runBlocking(stopAfter: TStopAfter): Unit

  def runAsync(stopAfter: TStopAfter): Future[Unit]

  /**
    * @return the current pareto frontier of solutions on this island?
    */
  def currentParetoFrontier(): TParetoFrontier[Solution]

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
  * Tells you how long to run an island for.
  */
trait TStopAfter {
  /**
    * Stop after the specified duration has elapsed.
    */
  def time: Duration
}

case class StopAfter(time: Duration) extends TStopAfter
