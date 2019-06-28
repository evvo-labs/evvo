package com.evvo.island.population

import com.evvo.island.population.HashingStrategy.HashingStrategy

/**
  * A solution, which has been scored by multiple fitness functions.
  *
  * @param score        Maps the name of fitness functions to the score of the solution with respect to them.
  * @param solution     The solution that has been scored.
  * @param hashStrategy How to hash this scored solution - on the score, or on the solution?
  * @tparam Sol The type of the solution that has been scored.
  */
case class Scored[Sol](score: Map[(String, OptimizationDirection), Double],
                       solution: Sol,
                       hashStrategy: HashingStrategy = HashingStrategy.ON_SCORES) {

  override def hashCode(): Int = hashStrategy match {
    case HashingStrategy.ON_SCORES => this.score.hashCode()
    case HashingStrategy.ON_SOLUTIONS => this.solution.hashCode()
  }

  override def equals(obj: Any): Boolean = obj match {
    case that: Scored[Sol] =>
      hashStrategy match {
        case HashingStrategy.ON_SCORES => this.score.equals(that.score)
        case HashingStrategy.ON_SOLUTIONS => this.solution.equals(that.solution)
      }
    case _ => false
  }
}


object HashingStrategy extends Enumeration {
  type HashingStrategy = Value
  val ON_SOLUTIONS, ON_SCORES = Value
}
