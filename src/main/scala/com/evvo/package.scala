package com

import com.evvo.island.population.TScored

package object evvo {
  type CreatorFunctionType[Sol] = () => TraversableOnce[Sol]
  type MutatorFunctionType[Sol] = IndexedSeq[TScored[Sol]] => TraversableOnce[Sol]
  type DeletorFunctionType[Sol] = IndexedSeq[TScored[Sol]] => TraversableOnce[TScored[Sol]]

  type ObjectiveFunctionType[Sol] = Sol => Double
}
