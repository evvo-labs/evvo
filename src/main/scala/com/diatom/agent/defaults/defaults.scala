/**
  * Provides default functions that are useful across multiple different applications of Evvo.
  * While it is possible to provide deletors that are useful for any problem type, because deletors
  * can work off of score alone, providing mutators and creators is harder. However, mutators
  * and creators for some common data representations of evocomp problems (bitstrings, vectors
  * of floats, etc) can also be provided.
  *
  */
package com.diatom.agent.defaults

import com.diatom.DeletorFunctionType
import com.diatom.agent.TDeletorFunc
import com.diatom.island.population.{Maximize, Minimize, ParetoFrontier, TScored}

/**
  * A deletor that deletes the dominated set, in a group of size `groupSize`
  *
  * @param numInputs the number of solutions to pull at a time
  */
case class DeleteDominated[Sol](numInputs: Int = 32) // scalastyle:ignore magic.number
  extends TDeletorFunc[Sol] {
  override val delete: DeletorFunctionType[Sol] = (sols: IndexedSeq[TScored[Sol]]) => {
    val nonDominatedSet = ParetoFrontier(sols).solutions
    sols.filterNot(elem => nonDominatedSet.contains(elem))
  }
  override val name = "DeleteDominated"
  override val shouldRunWithPartialInput: Boolean = true
}

case class DeleteWorstHalfByRandomObjective[Sol](numInputs: Int = 32)  // scalastyle:ignore magic.number
  extends TDeletorFunc[Sol] {

  override val delete: DeletorFunctionType[Sol] = (s: IndexedSeq[TScored[Sol]]) => {
    if (s.isEmpty) {
      s
    } else {
      val objectiveList = s.head.score.keys.toVector
      val objective = objectiveList(util.Random.nextInt(objectiveList.size))

      val ordering = objective match {
        case (_, Minimize) => Ordering.Double.reverse
        case (_, Maximize) => Ordering.Double
      }

      s.toVector.sortBy(_.score(objective))(ordering).take(s.size / 2).toSet
    }
  }
  override val name = "DeleteWorstHalfByRandomObjective"
  override val shouldRunWithPartialInput: Boolean = true
}
