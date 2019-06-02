package com.evvo.island

import java.util.Calendar

import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import com.evvo.{CreatorFunctionType, DeletorFunctionType, MutatorFunctionType, ObjectiveFunctionType}
import com.evvo.agent.{CreatorAgent, CreatorFunc, DeletorAgent, DeletorFunc, MutatorAgent, MutatorFunc, TCreatorFunc, TDeletorFunc, TMutatorFunc}
import com.evvo.island.population.{Minimize, Objective, Population, TObjective, TParetoFrontier}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global


class EvvoIsland[Sol]
(
  creators: Vector[TCreatorFunc[Sol]],
  mutators: Vector[TMutatorFunc[Sol]],
  deletors: Vector[TDeletorFunc[Sol]],
  fitnesses: Vector[TObjective[Sol]])
(implicit log: LoggingAdapter)
  extends TEvolutionaryProcess[Sol] {

  private val pop = Population(fitnesses)
  private val creatorAgents = creators.map(c => CreatorAgent(c, pop))
  private val mutatorAgents = mutators.map(m => MutatorAgent(m, pop))
  private val deletorAgents = deletors.map(d => DeletorAgent(d, pop))

  override def runAsync(stopAfter: TStopAfter)
  : Future[Unit] = {
    Future {
      log.info(s"Island running with stopAfter=${stopAfter}")

      // TODO can we put all of these in some combined pool? don't like having to manage each
      creatorAgents.foreach(_.start())
      mutatorAgents.foreach(_.start())
      deletorAgents.foreach(_.start())

      // TODO this is not ideal. fix wait time/add features to termination criteria
      val startTime = Calendar.getInstance().toInstant.toEpochMilli

      while (startTime + stopAfter.time.toMillis >
        Calendar.getInstance().toInstant.toEpochMilli) {
        Thread.sleep(500)
        val pareto = pop.getParetoFrontier()
        log.info(f"pareto = ${pareto}")
      }

      log.info(f"c=${startTime}, now=${Calendar.getInstance().toInstant.toEpochMilli}")

      stop()
    }
  }

  def runBlocking(stopAfter: TStopAfter): Unit = {
    Await.result(this.runAsync(stopAfter), Duration.Inf)
  }

  override def currentParetoFrontier(): TParetoFrontier[Sol] = {
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
  creators: Set[TCreatorFunc[Sol]] = Set[TCreatorFunc[Sol]](),
  mutators: Set[TMutatorFunc[Sol]] = Set[TMutatorFunc[Sol]](),
  deletors: Set[TDeletorFunc[Sol]] = Set[TDeletorFunc[Sol]](),
  objectives: Set[TObjective[Sol]] = Set[TObjective[Sol]]()
) {
  def addCreatorFromFunction(creatorFunc: CreatorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(creators = creators + CreatorFunc(creatorFunc, creatorFunc.toString))
  }

  def addCreator(creatorFunc: TCreatorFunc[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(creators = creators + creatorFunc)
  }

  def addMutatorFromFunction(mutatorFunc: MutatorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(mutators = mutators + MutatorFunc(mutatorFunc, mutatorFunc.toString))
  }

  def addMutator(mutatorFunc: TMutatorFunc[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(mutators = mutators + mutatorFunc)
  }

  def addDeletorFromFunction(deletorFunc: DeletorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(deletors = deletors + DeletorFunc(deletorFunc, deletorFunc.toString))
  }

  def addDeletor(deletorFunc: TDeletorFunc[Sol]): EvvoIslandBuilder[Sol] = {
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

  def buildLocalEvvo(): TEvolutionaryProcess[Sol] = {
    new LocalEvvoIsland[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      objectives.toVector)
  }
}
