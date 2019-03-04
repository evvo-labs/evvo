package com.diatom.agent

import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingReceive}
import com.diatom.population.TPopulation

import scala.util.Try

/**
  * The parent trait for all types of evolutionary agents.
  */
trait TAgent[Sol] {
  def start(): Unit
  def stop(): Unit
}

abstract class AAgent[Sol] extends TAgent[Sol] with Actor {
  override def receive: Receive = LoggingReceive(Logging.DebugLevel) {
    case StartAgent => start()
    case StopAgent => stop()
  }

  // consider factoring this out into a separate component and using
  // composition instead of inheriting a thread field. This will reduce the
  // number of tests needed by centralizing
  private val thread = new Thread {
    override def run(): Unit = {
      Try {
        while (!Thread.interrupted()) {
          step()
          Thread.sleep(3) // TODO: This represents an initial strategy, replace it.
        }
      }
    }
  }

  override def start(): Unit = {
    if (!thread.isAlive) {
      thread.start()
    } else {
      // TODO log a warning, you shouldn't be starting an already started agent
    }
  }

  override def stop(): Unit = {
    if (thread.isAlive) {
      thread.interrupt()
    } else {
      // TODO log a warning, you shouldn't be stopping an already stopped agent
    }
  }

  /**
    * Performs one operation on the population.
    */
  protected def step(): Unit
}

case object StartAgent
case object StopAgent

case class AgentActorRef[Sol](agent: ActorRef) extends TAgent[Sol] {
  override def start(): Unit = agent ! StartAgent

  override def stop(): Unit = agent ! StopAgent
}