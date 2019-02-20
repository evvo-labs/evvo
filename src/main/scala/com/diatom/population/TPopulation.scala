package com.diatom.population

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.diatom.agent.TFitnessFunc

import scala.collection.mutable

/**
  * A population is the set of all solutions current in an evolutionary process.
  *
  * @tparam Sol the type of the solutions in the population
  */
trait TPopulation[Sol] {
  /**
    * Adds the given solutions, if they are unique, subject to any additional constraints
    * imposed on the solutions by the population.
    *
    * @param solutions the solutions to add
    */
  def addSolutions(solutions: TraversableOnce[Sol]): Unit
}

/**
  * A interface to communicate with a population.
  *
  * @param popActor the actor ref to the population.
  * @tparam Sol the type of the solutions in the population
  */
case class PopulationActorRef[Sol](popActor: ActorRef) extends TPopulation[Sol] {

  /**
    * @param system           the actor system
    * @param fitnessFunctions the definition of fitness in this population.
    */
  def this(system: ActorSystem, fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]]) = {
    this(system.actorOf(PopulationActorRef.props(fitnessFunctions)))
  }

  override def addSolutions(solutions: TraversableOnce[Sol]): Unit = {
    popActor ! PopulationActorRef.AddSolutions(solutions)
  }

}

object PopulationActorRef {
  def props[Sol](fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]]): Props = {
    Props(Population(fitnessFunctions))
  }

  case class AddSolutions[Sol](solutions: TraversableOnce[Sol])
}




/**
  * The actor that maintains the population.
  *
  * @tparam Sol the type of the solutions in the population
  */
private case class Population[Sol](fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]])
  extends TPopulation[Sol] with Actor {

  import PopulationActorRef._

  private val pop = mutable.Set[Sol]()

  override def receive: Receive = {
    case AddSolutions(solutions: TraversableOnce[Sol]) => addSolutions(solutions)
  }

  override def addSolutions(solutions: TraversableOnce[Sol]): Unit = pop ++ solutions
}



