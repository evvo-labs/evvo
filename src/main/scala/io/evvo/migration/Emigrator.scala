package io.evvo.migration

import io.evvo.island.population.Scored

/**
  * Generic interface for handling emigrations.
  */
trait Emigrator[Sol] {

  /**
    * Sends the given solutions to the other islands.
    * @param solutions the solutions to send
    */
  def emigrate(solutions: Seq[Scored[Sol]]): Unit
}
