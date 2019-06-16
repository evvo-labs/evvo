/**
  * Provides default functions that are useful across multiple different applications of Evvo.
  * While it is possible to provide deletors that are useful for any problem type, because deletors
  * can work off of score alone, providing mutators and creators is harder. However, mutators
  * and creators for some common data representations of evocomp problems (bitstrings, vectors
  * of floats, etc) can also be provided.
  *
  */
package com.evvo.agent.defaults

import com.evvo.agent.{CreatorFunction, DeletorFunction, ModifierFunction}
import com.evvo.island.population
import com.evvo.island.population.{Maximize, Minimize, Scored}

/**
  * A deletor that deletes the dominated set, in a group of size `groupSize`
  *
  * @param numInputs the number of solutions to pull at a time
  */
case class DeleteDominated[Sol](override val numInputs: Int = 32) // scalastyle:ignore magic.number
  extends DeletorFunction[Sol]("DeleteDominated") {

  override def delete(sols: IndexedSeq[Scored[Sol]]): IndexedSeq[Scored[Sol]] = {
    val nonDominatedSet = population.ParetoFrontier(sols.toSet).solutions
    sols.filterNot(elem => nonDominatedSet.contains(elem))
  }
}

case class DeleteWorstHalfByRandomObjective[Sol](override val numInputs: Int = 32) // scalastyle:ignore magic.number
  extends DeletorFunction[Sol]("DeleteWorstHalfByRandomObjective") {

  override def delete(s: IndexedSeq[Scored[Sol]]): IndexedSeq[Scored[Sol]] = {
    if (s.isEmpty) {
      s
    } else {
      val objectiveList = s.head.score.keys.toVector
      val objective = objectiveList(util.Random.nextInt(objectiveList.size))

      val ordering = objective match {
        case (_, Minimize) => Ordering.Double.reverse
        case (_, Maximize) => Ordering.Double
      }

      s.toVector.sortBy(_.score(objective))(ordering).take(s.size / 2)
    }
  }
}

/**
  * A creator that generates `Bitstring`s by filling them with random bits.
  *
  * @param length         how long each `Bitstring` should be
  * @param proportionOnes the proportion of bits that start as 1
  */
case class BitstringGenerator(length: Int, proportionOnes: Double = 0.5)
  extends CreatorFunction[Bitstring]("BitstringGenerator") {
  override def create(): TraversableOnce[Seq[Boolean]] = {
    Vector.fill(32)(Vector.fill(length)(util.Random.nextDouble() < proportionOnes))
  }
}

/**
  * A mutator that swaps two random bits.
  *
  * @param numInputs The number of solutions to request in the contents of each
  *                  input set
  */
case class Bitswapper(override val numInputs: Int = 32)
  extends ModifierFunction[Bitstring]("Bitswapper") {
  override def modify(sols: IndexedSeq[Scored[Bitstring]]): TraversableOnce[Bitstring] = {
    sols.map(s => {
      val bitstring = s.solution
      val index1 = util.Random.nextInt(bitstring.length)
      val index2 = util.Random.nextInt(bitstring.length)

      // not mutation, doesn't need an intermediate temp variable
      bitstring.updated(index1, bitstring(index2)).updated(index2, bitstring(index1))
    })
  }
}

/**
  * A mutator for `Bitstring`s that flips a random bit.
  *
  * @param numInputs The number of solutions to request in the contents of each
  *                  input set
  */
case class Bitflipper(override val numInputs: Int = 32)
  extends ModifierFunction[Bitstring]("Bitflipper") {
  override def modify(sols: IndexedSeq[Scored[Bitstring]]): TraversableOnce[Bitstring] = {
    sols.map(s => {
      val bitstring = s.solution
      val index1 = util.Random.nextInt(bitstring.length)
      bitstring.updated(index1, !bitstring(index1))
    })
  }
}
