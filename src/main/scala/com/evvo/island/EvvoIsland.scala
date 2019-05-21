package com.evvo.island

import java.util.Calendar

import akka.event.LoggingAdapter
import com.evvo.agent.{CreatorAgent, DeletorAgent, MutatorAgent, TCreatorFunc, TDeletorFunc, TMutatorFunc}
import com.evvo.island.population.{Population, TObjective, TParetoFrontier}

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


  override def runAsync(terminationCriteria: TTerminationCriteria)
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

  def runBlocking(terminationCriteria: TTerminationCriteria): Unit = {
    Await.result(this.runAsync(terminationCriteria), Duration.Inf)
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
