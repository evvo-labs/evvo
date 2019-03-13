package com.diatom.population

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingReceive}
import akka.pattern.ask
import akka.util.Timeout
import com.diatom.agent.{PopulationInformation, TFitnessFunc, TPopulationInformation}
import com.diatom.population.PopulationActorRef.{GetInformation, GetParetoFrontier}
import com.diatom.{ParetoFrontier, Scored, TParetoFrontier, TScored}

import scala.collection.{TraversableOnce, mutable}
import scala.concurrent.Await
import scala.concurrent.duration._


// TODO this file is too large, split it into multiple

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
  def addSolutions(solutions: TraversableOnce[Sol])(implicit sender: ActorRef): Unit

  /**
    * Selects a random sample of the population.
    *
    * @param n the number of solutions.
    * @return n solutions.
    */
  def getSolutions(n: Int)(implicit sender: ActorRef): Set[TScored[Sol]]

  /**
    * Remove the given solutions from the population.
    *
    * @param solutions the solutions to remove
    */
  def deleteSolutions(solutions: TraversableOnce[TScored[Sol]])(implicit sender: ActorRef): Unit

  /**
    * @return the current pareto frontier of this population
    */
  def getParetoFrontier()(implicit sender: ActorRef = null): TParetoFrontier[Sol]

  /** @return a diagnostic report on this island, for agents to determine how often to run. */
  def getInformation()(implicit sender: ActorRef): TPopulationInformation
}

/**
  * A interface to communicate with a population.
  *
  * @param popActor the actor ref to the population.
  * @tparam Sol the type of the solutions in the population
  */
case class PopulationActorRef[Sol](popActor: ActorRef) extends TPopulation[Sol] {

  /** how long to wait for results, for non-unit-typed methods. */
  implicit private val timeout: Timeout = Timeout(5.seconds)

  override def addSolutions(solutions: TraversableOnce[Sol])(implicit sender: ActorRef): Unit = {
    popActor ! PopulationActorRef.AddSolutions(solutions)
  }


  override def getSolutions(n: Int)(implicit sender: ActorRef): Set[TScored[Sol]] = {
    val solutions = popActor ? PopulationActorRef.GetSolutions(n)
    Await.result(solutions, 5.seconds)
      .asInstanceOf[Set[TScored[Sol]]]
  }

  override def deleteSolutions(solutions: TraversableOnce[TScored[Sol]])(implicit sender: ActorRef): Unit = {
    popActor ! PopulationActorRef.DeleteSolutions(solutions)
  }

  override def getParetoFrontier()(implicit sender: ActorRef): TParetoFrontier[Sol] = {
    val paretoFuture = popActor ? GetParetoFrontier
    Await.result(paretoFuture, 5.seconds)
      .asInstanceOf[TParetoFrontier[Sol]]
  }

  override def getInformation()(implicit sender: ActorRef): TPopulationInformation = {
    // TODO test this (all all methods here?)
    // it appears that replacing the body of this method with "null" passes all tests.
    val infoFuture = popActor ? GetInformation
    Await.result(infoFuture, 5.seconds).asInstanceOf[TPopulationInformation]
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
  case object GetInformation
}

/**
  * The actor that maintains the population.
  *
  * @tparam Sol the type of the solutions in the population
  */
private case class Population[Sol](fitnessFunctionsIter: TraversableOnce[TFitnessFunc[Sol]])
  extends TPopulation[Sol] with Actor with ActorLogging {

  import PopulationActorRef._

  private val fitnessFunctions = fitnessFunctionsIter.toSet
  private val population = mutable.Set[TScored[Sol]]()
  private var populationVector = Vector[TScored[Sol]]()
  private var getSolutionIndex = 0

  override def receive: Receive = LoggingReceive(Logging.DebugLevel) {
    case AddSolutions(solutions: TraversableOnce[Sol]) => addSolutions(solutions)
    case GetSolutions(n: Int) => sender ! getSolutions(n)
    case DeleteSolutions(solutions: TraversableOnce[TScored[Sol]]) => deleteSolutions(solutions)
    case GetParetoFrontier => sender ! getParetoFrontier()
    case GetInformation => sender ! getInformation()
  }


  override def addSolutions(solutions: TraversableOnce[Sol])(implicit sender: ActorRef): Unit = {
    population ++= solutions.map(score)
    log.debug(f"Current population size ${population.size}")
  }

  private def score(solution: Sol): TScored[Sol] = {
    val scores = fitnessFunctions.map(func => {
      (func.toString, func.score(solution))
    }).toMap
    Scored(scores, solution)
  }


  override def getSolutions(n: Int)(implicit sender: ActorRef): Set[TScored[Sol]] = {
    // TODO: This can't be the final impl, inefficient space and time
    if (population.size <= n) {
      population.toSet // no need to randomize, all elements will be included anyway
    } else {
      var out = Set[TScored[Sol]]()
      while (out.size < n) {
        if (!populationVector.isDefinedAt(getSolutionIndex)) {
          populationVector = util.Random.shuffle(population.toVector)
          getSolutionIndex = 0
        }
        out += populationVector(getSolutionIndex)
        getSolutionIndex += 1
      }
      out
    }
  }

  override def deleteSolutions(solutions: TraversableOnce[TScored[Sol]])(implicit sender: ActorRef): Unit = {
    population --= solutions
  }

  override def getParetoFrontier()(implicit sender: ActorRef): TParetoFrontier[Sol] = {
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
            case ((name1, score1), (name2, score2)) => score1 <= score2
          })
      })) {
        // then, we have found a non-dominated solution, so add it to the output.
        out.add(sol)
      }
    }
    ParetoFrontier(out.toSet)
  }

  override def getInformation()(implicit sender: ActorRef): TPopulationInformation = {
    val out = PopulationInformation(population.size)
    log.debug(s"getInformation returning ${out}")
    out
  }
}

private object Population {
  //TODO figure out the exact convention around wrapper classes, companion objects, `from` methods
  /**
    * @return a Population scoring solutions by the given fitness functions.
    */
  def from[Sol](fitnessFunctions: TraversableOnce[TFitnessFunc[Sol]])
               (implicit system: ActorSystem)
  : ActorRef = {
    system.actorOf(Props(Population(fitnessFunctions)), s"Population${UUID.randomUUID()}")
  }
}



