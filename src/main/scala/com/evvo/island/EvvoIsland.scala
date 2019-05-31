package com.evvo.island

import java.util.Calendar

import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import com.evvo.agent._
import com.evvo.island.population.{Minimize, Objective, ParetoFrontier, Population, StandardPopulation}
import com.evvo.{CreatorFunctionType, DeletorFunctionType, MutatorFunctionType, ObjectiveFunctionType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class EvvoIsland[Sol]
(
  creators: Vector[CreatorFunction[Sol]],
  mutators: Vector[MutatorFunction[Sol]],
  deletors: Vector[DeletorFunction[Sol]],
  fitnesses: Vector[Objective[Sol]])
(implicit log: LoggingAdapter)
  extends EvolutionaryProcess[Sol] {

  private val pop: Population[Sol] = StandardPopulation(fitnesses)
  private val creatorAgents = creators.map(c => CreatorAgent(c, pop))
  private val mutatorAgents = mutators.map(m => MutatorAgent(m, pop))
  private val deletorAgents = deletors.map(d => DeletorAgent(d, pop))

  override def runAsync(terminationCriteria: TerminationCriteria)
  : Future[Unit] = {
    Future {
      log.info(s"Island running with terminationCriteria=${terminationCriteria}")

      // TODO can we put all of these in some combined pool? don't like having to manage each
      creatorAgents.foreach(_.start())
      mutatorAgents.foreach(_.start())
      deletorAgents.foreach(_.start())

      // TODO this is not ideal. fix wait time/add features to termination criteria
      val startTime = Calendar.getInstance().toInstant.toEpochMilli

      while (startTime + terminationCriteria.time.toMillis >
        Calendar.getInstance().toInstant.toEpochMilli) {
        Thread.sleep(500)
        val pareto = pop.getParetoFrontier()
        log.info(f"pareto = ${pareto}")
      }

      log.info(f"c=${startTime}, now=${Calendar.getInstance().toInstant.toEpochMilli}")

      stop()
    }
  }

  def runBlocking(terminationCriteria: TerminationCriteria): Unit = {
    Await.result(this.runAsync(terminationCriteria), Duration.Inf)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    pop.getParetoFrontier()
  }

  override def emigrate(solutions: Seq[Sol]): Unit = {
    pop.addSolutions(solutions)
  }

  override def poisonPill(): Unit = {
    stop()
  }

  private def stop(): Unit = {
    creatorAgents.foreach(_.stop())
    mutatorAgents.foreach(_.stop())
    deletorAgents.foreach(_.stop())
  }
}

object EvvoIsland {
  def builder[Sol](): EvvoIslandBuilder[Sol] = EvvoIslandBuilder[Sol]()
}


/**
  * @param creators   the functions to be used for creating new solutions.
  * @param mutators   the functions to be used for creating new solutions from current solutions.
  * @param deletors   the functions to be used for deciding which solutions to delete.
  * @param objectives the objective functions to maximize.
  */
case class EvvoIslandBuilder[Sol]
(
  creators: Set[CreatorFunction[Sol]] = Set[CreatorFunction[Sol]](),
  mutators: Set[MutatorFunction[Sol]] = Set[MutatorFunction[Sol]](),
  deletors: Set[DeletorFunction[Sol]] = Set[DeletorFunction[Sol]](),
  objectives: Set[Objective[Sol]] = Set[Objective[Sol]]()
) {
  def addCreatorFromFunction(creatorFunc: CreatorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(creators = creators + CreatorFunc(creatorFunc, creatorFunc.toString))
  }

  def addCreator(creatorFunc: CreatorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(creators = creators + creatorFunc)
  }

  def addMutatorFromFunction(mutatorFunc: MutatorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(mutators = mutators + MutatorFunc(mutatorFunc, mutatorFunc.toString))
  }

  def addMutator(mutatorFunc: MutatorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(mutators = mutators + mutatorFunc)
  }

  def addDeletorFromFunction(deletorFunc: DeletorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(deletors = deletors + DeletorFunc(deletorFunc, deletorFunc.toString))
  }

  def addDeletor(deletorFunc: DeletorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(deletors = deletors + deletorFunc)
  }

  // TODO this calling API is dangerous because it assumes minimization. it should be removed,
  //      but this will require refactoring across examples
  def addObjective(objective: ObjectiveFunctionType[Sol], name: String = "")
  : EvvoIslandBuilder[Sol] = {
    val realName = if (name == "") objective.toString() else name
    this.copy(objectives = objectives + Objective(objective, realName, Minimize))
  }

  def addObjective(objective: Objective[Sol])
  : EvvoIslandBuilder[Sol] = {
    this.copy(objectives = objectives + objective)
  }

  def toProps()(implicit system: ActorSystem): Props = {
    Props(new EvvoIslandActor[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      objectives.toVector))
  }

  def buildLocalEvvo(): EvolutionaryProcess[Sol] = {
    new LocalEvvoIsland[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      objectives.toVector)
  }
}
