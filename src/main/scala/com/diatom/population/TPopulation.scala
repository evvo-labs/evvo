package com.diatom.population

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.diatom.{ParetoFrontier, Scored, TParetoFrontier, TScored}
import com.diatom.agent.TFitnessFunc
import akka.pattern.ask
import akka.util.Timeout
import com.diatom.population.PopulationActorRef.GetParetoFrontier

import scala.concurrent.duration._
import scala.collection.{TraversableOnce, mutable}
import scala.concurrent.{Await, Awaitable, Future}

/**
  * A population is the set of all solutions current in an evolutionary process.
  *
  * @tparam Sol the type of the solutions in the population
  */
trait TPopulation[Sol] {
  // TODO add async calls for non-unit methods, that return a future
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

  /**
    * @return the current pareto frontier of this population
    */
  def getParetoFrontier(): TParetoFrontier[Sol]
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

  /** how long to wait for results, for non-unit-typed methods. */
  implicit private val timeout = Timeout(5.seconds)

  override def addSolutions(solutions: TraversableOnce[Sol]): Unit = {
    popActor ! PopulationActorRef.AddSolutions(solutions)
  }

  override def getSolutions(n: Int): Set[TScored[Sol]] = {
    val solutions = popActor ? PopulationActorRef.GetSolutions(n)
    Await.result(solutions, 5.seconds)
      .asInstanceOf[Set[TScored[Sol]]]
  }

  override def deleteSolutions(solutions: TraversableOnce[TScored[Sol]]): Unit = {
    popActor ! PopulationActorRef.DeleteSolutions(solutions)
  }

  override def getParetoFrontier(): TParetoFrontier[Sol] = {
    val paretoFuture = popActor ? GetParetoFrontier
    Await.result(paretoFuture, 5.seconds)
      .asInstanceOf[TParetoFrontier[Sol]]
  }
}

object PopulationActorRef {
  /**
    * @return a PopulationActorRef scoring solutions by the given fitness functions.
    */
  def from[Sol](fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]])
               (implicit system: ActorSystem)
  : TPopulation[Sol] = {
    PopulationActorRef(Population.from(fitnessFunctions))
  }

  def props[Sol](fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]]): Props = {
    Props(Population(fitnessFunctions))
  }

  case class AddSolutions[Sol](solutions: TraversableOnce[Sol])

  case class GetSolutions[Sol](n: Int)

  case class DeleteSolutions[Sol](solutions: TraversableOnce[TScored[Sol]])
  case object GetParetoFrontier
}

/**
  * The actor that maintains the population.
  *
  * @tparam Sol the type of the solutions in the population
  */
private case class Population[Sol](fitnessFunctionsIter: TraversableOnce[TFitnessFunc[Sol]])
  extends TPopulation[Sol] with Actor {

  import PopulationActorRef._
  private val fitnessFunctions = fitnessFunctionsIter.toSet
  private val population = mutable.Set[TScored[Sol]]()

  override def receive: Receive = {
    case AddSolutions(solutions: TraversableOnce[Sol]) => addSolutions(solutions)
    case GetSolutions(n: Int) => sender ! getSolutions(n)
    case DeleteSolutions(solutions: TraversableOnce[TScored[Sol]]) => deleteSolutions(solutions)
    case GetParetoFrontier => sender ! getParetoFrontier()
    case x: Any => throw new IllegalArgumentException(s"bad message: ${x}")
  }


  override def addSolutions(solutions: TraversableOnce[Sol]): Unit = {
    population ++= solutions.map(score)
  }

  private def score(solution: Sol): TScored[Sol] = {
    val scores = fitnessFunctions.map(func => {
      (func.toString, func.score(solution))
    }).toMap
    Scored(scores, solution)
  }


  override def getSolutions(n: Int): Set[TScored[Sol]] = {
    // TODO: This can't be the final impl, inefficient space and time
    util.Random.shuffle(population.toVector).take(n).toSet
  }

  override def deleteSolutions(solutions: TraversableOnce[TScored[Sol]]): Unit = {
    population --= solutions
  }

  override def getParetoFrontier(): TParetoFrontier[Sol] = {
    // TODO test this for performance, and optimize - this is likely to become a bottleneck
    // https://static.aminer.org/pdf/PDF/000/211/201/on_the_computational_complexity_of_finding_the_maxima_of_a.pdf

    // mutable set used here for performance, converted back to immutable afterwards
    val out: mutable.Set[TScored[Sol]] = mutable.Set()

    for (sol <- population) {
      // if, for all other elements in the population..
      if (population.forall(other => {
        // (this part just ignores the solution that we are looking at)
        sol == other ||
        // there is at least one score that this solution beats other solutions at,
        // (then, that is the dimension along which this solution is non dominated)
        sol.score.zip(other.score).exists({
          case ((name1, score1), (name2, score2)) => score1 > score2
        })
      })) {
        // then, we have found a non-dominated solution, so add it to the output.
        out.add(sol)
      }
    }

    ParetoFrontier(out.toSet)
  }
}

private object Population {
  /**
    * @return a Population scoring solutions by the given fitness functions.
    */
  def from[Sol](fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]])
               (implicit system: ActorSystem)
  : ActorRef = {
    system.actorOf(Props(Population(fitnessFunctions)))
  }
}



