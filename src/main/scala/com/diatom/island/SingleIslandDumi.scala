package com.diatom.island

import com.diatom.{CreatorFunc, DeletorFunc, MutatorFunc}
import com.diatom.agent.{CreatorAgent, MutatorAgent, TFitnessFunction}

case class SingleIslandDumi[Sol](creators:  Vector[CreatorFunc[Sol]],
                                 mutators:  Vector[MutatorFunc[Sol]],
                                 deletors:  Vector[DeletorFunc[Sol]],
                                 fitnesses: Vector[TFitnessFunction[Sol]]) {

  def run(): Set[Sol] = {
    val creatorAgents = creators.map(c => CreatorAgent(c))
    val mutatorAgents = mutators.map(m => MutatorAgent(m))
  }
}

object SingleIslandDumi {
  def apply[Sol](creators: TraversableOnce[CreatorFunc[Sol]],
                 mutators: TraversableOnce[MutatorFunc[Sol]],
                 deletors: TraversableOnce[DeletorFunc[Sol]],
                 fitnesses: TraversableOnce[TFitnessFunction[Sol]])
  : SingleIslandDumi[Sol] = {
    new SingleIslandDumi[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      fitnesses.toVector)
  }
}