package com.diatom.agent

import com.diatom.DeletorFunctionType
import com.diatom.island.population.{ParetoFrontier, TScored}
import com.diatom.professormatching.ProfessorMatching.PMSolution

/**
  * Provides default functions that are useful across multiple different applications of Evvo.
  * While it is possible to provide deletors that are useful for any problem type, because deletors
  * can work off of score alone, providing mutators and creators is harder. However, mutators
  * and creators for some common data representations of evocomp problems (bitstrings, vectors
  * of floats, etc) can also be provided.
  *
  */
object default {

  /**
    * A deletor that deletes the dominated set, in a group of size `groupSize`
    * @param groupSize the number of solutions to pull at a time
    */
  case class DeleteDominated[Sol](groupSize: Int) extends TDeletorFunc[Sol] {
    override def delete: DeletorFunctionType[Sol] = (sols: IndexedSeq[TScored[Sol]]) => {
      val nonDominatedSet = ParetoFrontier(sols).solutions
      sols diff nonDominatedSet.toVector
    }

    override def numInputs: Int = groupSize
  }

  case class DeleteWorstHalfByRandomObjective[Sol](groupSize: Int = 32) extends TDeletorFunc[Sol] {
    override def delete: DeletorFunctionType[Sol] = (s: IndexedSeq[TScored[Sol]]) => {
      if (s.isEmpty) {
        s
      } else {
        val funcs = s.head.score.keys.toVector
        val func = funcs(util.Random.nextInt(funcs.size))

        s.toVector.sortBy(_.score(func)).take(s.size / 2).toSet
      }
    }

    override def numInputs: Int = groupSize
  }
}