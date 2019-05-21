package com.evvo.island.population

import scala.collection.mutable

/**
  * Represents a set of solutions that are non-dominated.
  */
trait TParetoFrontier[Sol] {
  //TODO replace with IndexedSeq and provide alternate hashing here
  def solutions: Set[TScored[Sol]]
}

class ParetoFrontier[Sol](private val inputSolutions: TraversableOnce[TScored[Sol]])
  extends TParetoFrontier[Sol] {

  // TODO test this for performance, and optimize - this is likely to become a bottleneck
  // https://static.aminer.org/pdf/PDF/000/211/201/on_the_computational_complexity_of_finding_the_maxima_of_a.pdf

  private val paretoFrontier = {
    // mutable set used here for performance, converted back to immutable afterwards
    val out: mutable.Set[TScored[Sol]] = mutable.Set()

    for (sol <- inputSolutions) {
      // if, for all other elements in the population..
      if (inputSolutions.forall(other => {
        // (this part just ignores the solution that we are looking at)
        sol == other ||
          // there is at least one score that this solution beats other solutions at,
          // (then, that is the dimension along which this solution is non dominated)
          sol.score.zip(other.score).exists({
            case (((name1, direction), score1), ((name2, _), score2)) => direction match {
              case Minimize => score1 < score2
              case Maximize => score1 > score2
            }
          }) ||
          sol.score.zip(other.score).forall({
            case ((name1, score1), (name2, score2)) => score1 == score2
          })
      })) {
        // then, we have found a non-dominated solution, so add it to the output.
        out.add(sol)
      }
    }
    out.toSet
  }

  override def solutions: Set[TScored[Sol]] = paretoFrontier

  override def toString: String = {
    val contents = paretoFrontier.map(_.score.toVector.map {
      case ((name, dir), score) => name -> score
    }.toMap).mkString("\n  ")
    f"ParetoFrontier(\n  ${contents})"
  }
}

object ParetoFrontier {
  def apply[Sol](solutions: TraversableOnce[TScored[Sol]]): ParetoFrontier[Sol] = new ParetoFrontier(solutions)
}
