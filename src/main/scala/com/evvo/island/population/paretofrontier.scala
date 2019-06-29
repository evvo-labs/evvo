package com.evvo.island.population

import scala.collection.mutable

/**
  * A set of solutions, none of which dominate each other.
  */
case class ParetoFrontier[Sol](solutions: Set[Scored[Sol]]) {
  if (!ParetoFrontier.isParetoFrontier(solutions)) {
    throw new IllegalArgumentException(
      s"""
         |`solutions` must be valid pareto frontier, was ${solutions}
         |Likely, you used new `ParetoFrontier(…)` instead of `ParetoFrontier(…)`
       """.stripMargin)
  }

  override def toString: String = {
    val contents = solutions.map(_.score.toVector.map {
      case ((name, dir), score) => name -> score
    }.toMap).mkString("\n  ")
    f"ParetoFrontier(\n  ${contents})"
  }

  /** @return Whether this Pareto frontier contains a point that dominates the given solution. */
  def dominates(sol: Scored[Sol]): Boolean = {
    this.solutions.exists(_.dominates(sol))
  }

  /** @return Whether this Pareto frontier is domianted by the given solution. */
  def dominatedBy(sol: Scored[Sol]): Boolean = {
    this.solutions.isEmpty || this.solutions.exists(sol.dominates)
  }
}

object ParetoFrontier {
  def apply[Sol](solutions: Set[Scored[Sol]]): ParetoFrontier[Sol] = {
    new ParetoFrontier(setToParetoFrontier(solutions))
  }

  // TODO test this for performance, and optimize - this is likely to become a bottleneck
  // https://static.aminer.org/pdf/PDF/000/211/201/on_the_computational_complexity_of_finding_the_maxima_of_a.pdf
  def setToParetoFrontier[Sol](solutions: Set[Scored[Sol]]): Set[Scored[Sol]] = {
    solutions.filterNot(s => solutions.exists(_.dominates(s)))
  }

  def isParetoFrontier[Sol](solutions: Set[Scored[Sol]]): Boolean = {
    solutions == ParetoFrontier.setToParetoFrontier(solutions)
  }
}
