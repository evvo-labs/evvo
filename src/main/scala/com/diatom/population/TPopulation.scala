package com.diatom.population

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.diatom.TScored
import com.diatom.agent.TFitnessFunc
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.collection.{TraversableOnce, mutable}
import scala.concurrent.Await

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

  /**
    * Selects a random sample of the population.
    *
    * @param n the number of solutions.
    * @return n solutions.
    */
  def getSolutions(n: Int): Set[TScored[Sol]]

  /**
    * Remove the given solutions from the population.
    *
    * @param solutions the solutions to remove
    */
  def deleteSolutions(solutions: TraversableOnce[TScored[Sol]]): Unit

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

  override def getSolutions(n: Int): Set[TScored[Sol]] = {
    implicit val timeout = Timeout(5.seconds)
    val solutions = popActor ? PopulationActorRef.GetSolutions(n)
    Await.result(solutions, 5.seconds).asInstanceOf[Set[TScored[Sol]]]
  }

  override def deleteSolutions(solutions: TraversableOnce[TScored[Sol]]): Unit = {
    popActor ! PopulationActorRef.DeleteSolutions(solutions)
  }
}

object PopulationActorRef {
  def props[Sol](fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]]): Props = {
    Props(Population(fitnessFunctions))
  }

  case class AddSolutions[Sol](solutions: TraversableOnce[Sol])

  case class GetSolutions[Sol](n: Int)

  case class DeleteSolutions[Sol](solutions: TraversableOnce[TScored[Sol]])

}


/**
  * The actor that maintains the population.
  *
  * @tparam Sol the type of the solutions in the population
  */
private case class Population[Sol](fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]])
  extends TPopulation[Sol] with Actor {

  import PopulationActorRef._

  private val pop = mutable.Set[TScored[Sol]]()

  override def receive: Receive = {
    case AddSolutions(solutions: TraversableOnce[Sol]) => addSolutions(solutions)
    case GetSolutions(n: Int) => sender ! getSolutions(n)
    case DeleteSolutions(solutions: TraversableOnce[TScored[Sol]]) => deleteSolutions(solutions)
  }


  override def addSolutions(solutions: TraversableOnce[Sol]): Unit = pop ++ solutions

  override def getSolutions(n: Int): Set[TScored[Sol]] = {
    // TODO: This can't be the final impl, inefficient space and time
    util.Random.shuffle(pop).take(n).toSet
  }

  override def deleteSolutions(solutions: TraversableOnce[TScored[Sol]]): Unit = {
    pop -- solutions
  }
}



