package io.evvo.migration

import io.evvo.island.population.Scored

/**
  * Generic interface for handling immigration.
  */
trait Immigrator[Sol] {
  // TODO this could probably be better done with a callback called whenever there are new
  //      solutions, instead of just returning solutions and making someone else handle
  //      the post-processing and timing
  /** @return up to `numberOfImmigrants` solutions that are waiting to be immigrated.*/
  def immigrate(numberOfImmigrants: Int): Seq[Scored[Sol]]
}
