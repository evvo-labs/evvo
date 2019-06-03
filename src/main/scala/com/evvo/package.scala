package com

import com.evvo.island.population.Scored

package object evvo {
  type CreatorFunctionType[Sol] = () => TraversableOnce[Sol]
  type MutatorFunctionType[Sol] = IndexedSeq[Scored[Sol]] => TraversableOnce[Sol]
  type DeletorFunctionType[Sol] = IndexedSeq[Scored[Sol]] => TraversableOnce[Scored[Sol]]

  type ObjectiveFunctionType[Sol] = Sol => Double
}
