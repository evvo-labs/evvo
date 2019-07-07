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
  /** @return Whether this solution dominates that one. */
  def dominates(other: Scored[Sol]): Boolean = {
    // There's likely more work to be done here, as zipping together two
    // maps may not guarantee the same ordering. We don't want the ordering
    // to matter, so we should iterate over known names after checking that the
    // name set are equivalent.

    // This works by checking if there exists one score where this solution
    // (which has score1) outperforms other (which has score2).
    this.score.zip(other.score).exists({
      case (((name1, direction), score1), ((name2, _), score2)) => direction match {
        case Minimize => score1 < score2
        case Maximize => score1 > score2
      }
    })
  }

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
