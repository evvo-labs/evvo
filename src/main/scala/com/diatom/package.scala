package com

import com.diatom.island.population.TScored

package object diatom {
  type CreatorFunctionType[Sol] = () => TraversableOnce[Sol]
  type MutatorFunctionType[Sol] = IndexedSeq[TScored[Sol]] => TraversableOnce[Sol]
  type DeletorFunctionType[Sol] = IndexedSeq[TScored[Sol]] => TraversableOnce[TScored[Sol]]
  type FitnessFunctionType[Sol] = Sol => Double
}