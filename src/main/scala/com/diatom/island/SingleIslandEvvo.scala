package com.diatom.island

import akka.actor.ActorSystem
import com.diatom.agent.func._
import com.diatom.agent._
import com.diatom._
import com.diatom.population.{PopulationActorRef, TPopulation}


import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * A single-island evolutionary system, which will run on one computer (although on multiple
  * CPU cores).
  */
case class SingleIslandEvvo[Sol](creators: Vector[TCreatorFunc[Sol]],
                                 mutators: Vector[TMutatorFunc[Sol]],
                                 deletors: Vector[TDeletorFunc[Sol]],
                                 fitnesses: Vector[TFitnessFunc[Sol]]) extends TIsland[Sol] {

  val system: ActorSystem = ActorSystem("evvo")


  def run(terminationCriteria: TTerminationCriteria): TParetoFrontier[Sol] = {

    val pop: TPopulation[Sol] = new PopulationActorRef[Sol](system, fitnesses)
    val creatorAgents = creators.map(c => CreatorAgent(c, pop))
    val mutatorAgents = mutators.map(m => MutatorAgent(m, pop))
    val deletorAgents = deletors.map(d => DeletorAgent(d, pop))

    creatorAgents.foreach(_.start())
    mutatorAgents.foreach(_.start())
    deletorAgents.foreach(_.start())

    Thread.sleep(terminationCriteria.time.toMillis)

    creatorAgents.foreach(_.stop())
    mutatorAgents.foreach(_.stop())
    deletorAgents.foreach(_.stop())

    pop.getParetoFrontier()
  }


  override def forceKill(): Unit = {
    // TODO test & implement forceKill
  }

}

object SingleIslandEvvo {
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
  : SingleIslandEvvo[Sol] = {
    new SingleIslandEvvo[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      fitnesses.toVector)
  }

  def builder[Sol](): SingleIslandEvvoBuilder[Sol] = new SingleIslandEvvoBuilder[Sol]()

}

/**
  * @param creators  the functions to be used for creating new solutions.
  * @param mutators  the functions to be used for creating new solutions from current solutions.
  * @param deletors  the functions to be used for deciding which solutions to delete.
  * @param fitnesses the objective functions to maximize.
  */
case class SingleIslandEvvoBuilder[Sol](creators: Set[TCreatorFunc[Sol]],
                                        mutators: Set[TMutatorFunc[Sol]],
                                        deletors: Set[TDeletorFunc[Sol]],
                                        fitnesses: Set[TFitnessFunc[Sol]]
                                       ) {

  def this() = {
    this(Set(), Set(), Set(), Set())
  }

  def addCreator(creatorFunc: CreatorFunctionType[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(creators = creators + CreatorFunc(creatorFunc))
  }

  def addMutator(mutatorFunc: MutatorFunctionType[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(mutators = mutators + MutatorFunc(mutatorFunc))
  }

  def addDeletor(deletorFunc: DeletorFunctionType[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(deletors = deletors + DeletorFunc(deletorFunc))
  }

  def addFitness(fitnessFunc: FitnessFunctionType[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(fitnesses = fitnesses + FitnessFunc(fitnessFunc))
  }

  def build(): SingleIslandEvvo[Sol] = {
    SingleIslandEvvo[Sol](creators, mutators, deletors, fitnesses)
  }

}