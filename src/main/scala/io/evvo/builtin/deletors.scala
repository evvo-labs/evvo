package io.evvo.builtin

import io.evvo.agent.DeletorFunction
import io.evvo.island.population.{Maximize, Minimize, ParetoFrontier, Scored}

/** Provides default functions that are useful for any datatype. */
object deletors {
  /** A deletor that deletes the dominated set, in a group of size `groupSize`
    *
    * @param numInputs the number of solutions to pull at a time
    */
  case class DeleteDominated[Sol](override val numInputs: Int = 32)
    extends DeletorFunction[Sol]("DeleteDominated") {

    override def delete(sols: IndexedSeq[Scored[Sol]]): IndexedSeq[Scored[Sol]] = {
      // If it's not in the non-dominated set, then it is dominated.
      val nonDominatedSet = ParetoFrontier(sols.toSet).solutions
      sols.filterNot(nonDominatedSet.contains)
    }
  }

  /** Picks a random objective, then grabs `numInputs` solutions and removes the worst half,
    * as measured by that objective.
    *
    * @param numInputs The number of solutions to request in the contents of each
    *                  input set
    */
  case class DeleteWorstHalfByRandomObjective[Sol](override val numInputs: Int = 32)
    extends DeletorFunction[Sol]("DeleteWorstHalfByRandomObjective") {

    override def delete(s: IndexedSeq[Scored[Sol]]): IndexedSeq[Scored[Sol]] = {
      if (s.isEmpty) {
        s
      } else {
        val objectiveList = s.head.score.keys.toVector
        val objective = objectiveList(util.Random.nextInt(objectiveList.size))

        val ordering = objective match {
          case (_, Minimize) => Ordering.Double.TotalOrdering.reverse
          case (_, Maximize) => Ordering.Double.TotalOrdering
        }

        s.toVector.sortBy(_.score(objective))(ordering).take(s.size / 2)
      }
    }
  }
}
