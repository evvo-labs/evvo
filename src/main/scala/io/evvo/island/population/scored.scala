package io.evvo.island.population

import io.evvo.island.population.HashingStrategy.HashingStrategy

/** A solution, which has been scored by multiple fitness functions.
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

    // This works by checking if all scores are better than or equal to the other,
    // and also there is at least one that doesn't tie. We know that if there is a tie,
    // this solution outperforms that solution, otherwise the first expression would
    // evaluate to false. The second expression (after the &&) is needed to make sure
    // that solutions that are equal don't dominate each other.
    val zippedScores = this.score.zip(other.score)
    zippedScores.forall({
      case (((name1, direction), score1), ((name2, _), score2)) => direction match {
        case Minimize => score1 <= score2
        case Maximize => score1 >= score2
      }
    }) &&
    zippedScores.exists({
      case (((name1, direction), score1), ((name2, _), score2)) => direction match {
        case Minimize => score1 != score2
        case Maximize => score1 != score2
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
