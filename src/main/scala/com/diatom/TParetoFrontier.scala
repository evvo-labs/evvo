package com.diatom


import scala.collection.mutable

/**
  * Represents a set of solutions that are non-dominated.
  */
trait TParetoFrontier[Sol] {
  def solutions: Set[TScored[Sol]]
}

class ParetoFrontier[Sol](private val _solutions: Set[TScored[Sol]]) extends TParetoFrontier[Sol] {

  // TODO test this for performance, and optimize - this is likely to become a bottleneck
  // https://static.aminer.org/pdf/PDF/000/211/201/on_the_computational_complexity_of_finding_the_maxima_of_a.pdf

  // mutable set used here for performance, converted back to immutable afterwards

  private val paretoFrontier = {
    val out: mutable.Set[TScored[Sol]] = mutable.Set()

    for (sol <- _solutions) {
      // if, for all other elements in the population..
      if (_solutions.forall(other => {
        // (this part just ignores the solution that we are looking at)
        sol == other ||
          // there is at least one score that this solution beats other solutions at,
          // (then, that is the dimension along which this solution is non dominated)
          sol.score.zip(other.score).exists({
            case ((name1, score1), (name2, score2)) => score1 <= score2
          })
      })) {
        // then, we have found a non-dominated solution, so add it to the output.
        out.add(sol)
      }
    }
    out.toSet
  }

  override def solutions: Set[TScored[Sol]] = paretoFrontier

  override def toString: String = f"ParetoFrontier(${paretoFrontier}"
}

object ParetoFrontier {
  def apply[Sol](solutions: Set[TScored[Sol]]): ParetoFrontier[Sol] = new ParetoFrontier(solutions)
}