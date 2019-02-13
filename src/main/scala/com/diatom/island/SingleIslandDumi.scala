package com.diatom.island

import com.diatom.agent.func._
import com.diatom.agent.{FitnessFunc, TFitnessFunc}
import com.diatom.{CreatorFunctionType, DeletorFunctionType, FitnessFunctionType, MutatorFunctionType}

case class SingleIslandDumi[Sol](creators: Vector[TCreatorFunc[Sol]],
                                 mutators: Vector[TMutatorFunc[Sol]],
                                 deletors: Vector[TDeletorFunc[Sol]],
                                 fitnesses: Vector[TFitnessFunc[Sol]]) {

  def run(): Set[Sol] = {
    Set()
    // val creatorAgents = creators.map(c => CreatorAgent(c))
    // val mutatorAgents = mutators.map(m => MutatorAgent(m))
    // val deletorAgents = mutators.map(d => DeletorAgent(d))
  }
}

object SingleIslandDumi {
  /**
    * @param creators  the functions to be used for creating new solutions.
    * @param mutators  the functions to be used for creating new solutions from current solutions.
    * @param deletors  the functions to be used for deciding which solutions to delete.
    * @param fitnesses the objective functions to maximize.
    */
  def apply[Sol](creators: TraversableOnce[TCreatorFunc[Sol]],
                 mutators: TraversableOnce[TMutatorFunc[Sol]],
                 deletors: TraversableOnce[TDeletorFunc[Sol]],
                 fitnesses: TraversableOnce[TFitnessFunc[Sol]])
  : SingleIslandDumi[Sol] = {
    new SingleIslandDumi[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      fitnesses.toVector)
  }

  def builder[Sol](): SingleIslandDumiBuilder[Sol] = new SingleIslandDumiBuilder[Sol]()

}

/**
  * @param creators  the functions to be used for creating new solutions.
  * @param mutators  the functions to be used for creating new solutions from current solutions.
  * @param deletors  the functions to be used for deciding which solutions to delete.
  * @param fitnesses the objective functions to maximize.
  */
case class SingleIslandDumiBuilder[Sol](creators: Set[TCreatorFunc[Sol]],
                                        mutators: Set[TMutatorFunc[Sol]],
                                        deletors: Set[TDeletorFunc[Sol]],
                                        fitnesses: Set[TFitnessFunc[Sol]]
                                       ) {

  def this() = {
    this(Set(), Set(), Set(), Set())
  }

  def addCreator(creatorFunc: CreatorFunctionType[Sol]): SingleIslandDumiBuilder[Sol] = {
    this.copy(creators = creators + CreatorFunc(creatorFunc))
  }

  def addMutator(mutatorFunc: MutatorFunctionType[Sol]): SingleIslandDumiBuilder[Sol] = {
    this.copy(mutators = mutators + MutatorFunc(mutatorFunc))
  }

  def addDeletor(deletorFunc: DeletorFunctionType[Sol]): SingleIslandDumiBuilder[Sol] = {
    this.copy(deletors = deletors + DeletorFunc(deletorFunc))
  }

  def addFitness(fitnessFunc: FitnessFunctionType[Sol]): SingleIslandDumiBuilder[Sol] = {
    this.copy(fitnesses = fitnesses + FitnessFunc(fitnessFunc))
  }

  def build(): SingleIslandDumi[Sol] = {
    SingleIslandDumi[Sol](creators, mutators, deletors, fitnesses)
  }

}