package io.evvo.island.population

import scala.util.chaining._

/** A set of solutions, none of which dominate each other.
  *
  * @param solutions A non-dominated set.
  */
case class ParetoFrontier[Sol] private (solutions: Set[Scored[Sol]]) {
  if (!ParetoFrontier.isParetoFrontier(solutions)) {
    throw new IllegalArgumentException(s"""
         |`solutions` must be valid pareto frontier, was ${solutions}
         |Likely, you used new `ParetoFrontier(…)` instead of `ParetoFrontier(…)`
       """.stripMargin)
  }

  override def toString: String = {
    val contents = solutions
      .map(_.score.toVector.map {
        case ((name, dir), score) => name -> score
      }.toMap)
      .mkString("\n  ")
    f"ParetoFrontier(\n  ${contents})"
  }

  /** @param sortByObjective The objective to sort by.
    * @return A table-formatted string of the scores in the Pareto frontier.
    */
  def toTable(sortByObjective: String = ""): String = {
    val objectives = solutions.head.score.keys.map(_._1).toVector
    // This is the index in objectives to use as the sort key, either the index of the
    // provided objective or the first objective
    val sortByKey = objectives
      .indexOf(sortByObjective)
      .pipe(
        (x: Int) =>
          if (x != -1) {
            x
          } else {
            0
        }
      )

    val stringifiedSolutions = solutions.toIndexedSeq
    // Sort by the first objective, for some semblance of order
      .map(s => objectives.map(s.scoreOn))
      .sortBy(_(sortByKey))(Ordering.Double.TotalOrdering)
      .map(_.mkString("\t"))
      .mkString("\n")

    s"""
       |Pareto Frontier:
       |------------------------------------------------------------
       |${objectives.mkString("\t")}
       |$stringifiedSolutions
      """.stripMargin
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

  /** @param solutions The solutions set to create the pareto frontier from.
    * @return A ParetoFrontier with the non-dominated solutions from the given set.
    */
  def apply[Sol](solutions: Set[Scored[Sol]]): ParetoFrontier[Sol] = {
    new ParetoFrontier(setToParetoFrontier(solutions))
  }

  // TODO test this for performance, and optimize - this is likely to become a bottleneck
  // https://static.aminer.org/pdf/PDF/000/211/201/on_the_computational_complexity_of_finding_the_maxima_of_a.pdf
  def setToParetoFrontier[Sol](solutions: Set[Scored[Sol]]): Set[Scored[Sol]] = {
    solutions.filterNot(s => solutions.exists(_.dominates(s)))
  }

  /** Is the given set of solutions a Pareto Frontier? That is, does it contain only
    * non-dominated solutions?
    */
  def isParetoFrontier[Sol](solutions: Set[Scored[Sol]]): Boolean = {
    solutions == ParetoFrontier.setToParetoFrontier(solutions)
  }
}
