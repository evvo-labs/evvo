package com.evvo.island

import java.util.Calendar

import akka.event.LoggingAdapter
import com.evvo.agent.{CreatorAgent, DeletorAgent, MutatorAgent, TCreatorFunc, TDeletorFunc, TMutatorFunc}
import com.evvo.island.population.{Population, TObjective, TParetoFrontier}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


/**
  * An island that can be run locally. Does not connect to any other networked island, but is good
  * for testing agent functions without having to spin up a cluster.
  */
class LocalEvvoIsland[Sol]
(
  creators: Vector[TCreatorFunc[Sol]],
  mutators: Vector[TMutatorFunc[Sol]],
  deletors: Vector[TDeletorFunc[Sol]],
  fitnesses: Vector[TObjective[Sol]]
) extends TEvolutionaryProcess[Sol] {
  implicit val log: LoggingAdapter = LocalLogger
  private val island = new EvvoIsland(creators,mutators,deletors,fitnesses)

  override def runBlocking(terminationCriteria: TTerminationCriteria): Unit = {
    island.runBlocking(terminationCriteria)
  }

  override def runAsync(terminationCriteria: TTerminationCriteria): Future[Unit] = {
    island.runAsync(terminationCriteria)
  }

  override def currentParetoFrontier(): TParetoFrontier[Sol] = {
    island.currentParetoFrontier()
  }

  override def emigrate(solutions: Seq[Sol]): Unit = {
    island.emigrate(solutions)
  }

  override def poisonPill(): Unit = {
    island.poisonPill()
  }
}


object LocalLogger extends LoggingAdapter {
  override def isErrorEnabled: Boolean = true

  override def isWarningEnabled: Boolean = true

  override def isInfoEnabled: Boolean = true

  override def isDebugEnabled: Boolean = true

  override protected def notifyError(message: String): Unit = {
    println("[ERROR] " + message)
  }

  override protected def notifyError(cause: Throwable, message: String): Unit = {
    println("[INFO] " + message + " caused by " + cause.toString)
  }

  override protected def notifyWarning(message: String): Unit = {
    println("[WARN ] " + message)
  }

  override protected def notifyInfo(message: String): Unit = {
    println("[INFO ] " + message)
  }

  override protected def notifyDebug(message: String): Unit = {
    println("[DEBUG] " + message)
  }
}
