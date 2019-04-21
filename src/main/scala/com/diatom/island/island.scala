package com.diatom.island

import com.diatom.TParetoFrontier

import scala.concurrent.duration.Duration

/**
  * An evolutionary process with a set of actors that all act upon one set of solutions.
  */
trait TIsland[Solution] {
  def run(terminationCriteria: TTerminationCriteria): TParetoFrontier[Solution]
}


trait TTerminationCriteria {
  def time: Duration
}

case class TerminationCriteria(time: Duration) extends TTerminationCriteria
