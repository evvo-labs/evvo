package com.diatom.agent

import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.{Logging, LoggingReceive}
import akka.util.Timeout
import scala.concurrent.duration._
import com.diatom.population.TPopulation

import scala.concurrent.Await
import scala.util.Try

/**
  * The parent trait for all types of evolutionary agents.
  */
trait TAgent[Sol] {
  def start(): Unit
  def stop(): Unit
  def numInvocations: Int
}

// TODO all of the children classes have to pass a strategy and population,
// which are also available in their top-level under different names, unless they override?
// should look into abstract classes and case classes, and figure out what to do
abstract class AAgent[Sol](protected val strategy: TAgentStrategy,
                           protected val population: TPopulation[Sol])
  extends TAgent[Sol] with Actor with ActorLogging {
  var numInvocations: Int = 0


  override def receive: Receive = LoggingReceive(Logging.DebugLevel) {
    case StartAgent => start()
    case StopAgent => stop()
    case GetNumInvocations => sender ! numInvocations
  }

  // consider factoring this out into a separate component and using
  // composition instead of inheriting a thread field. This will reduce the
  // number of tests needed by centralizing
  private val thread = new Thread {
    override def run(): Unit = {
      var waitTime: Duration = 0.millis
      Try {
        while (!Thread.interrupted()) {
          try {
            numInvocations += 1
            step()
          } catch {
            case e: Exception => {
              log.error(e.toString)
            }
          }

          Thread.sleep(waitTime.toMillis)

          if (numInvocations % 100 == 0) {
            // TODO this is blocking, on population.getInformation(), can't be in main loop
            val nextInformation = population.getInformation()
            waitTime = strategy.waitTime(nextInformation)
            log.debug(s"new waitTime: ${waitTime}")
          }
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
    log.debug(s"stopping after ${numInvocations} invocations")
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
case object GetNumInvocations

case class AgentActorRef[Sol](agent: ActorRef) extends TAgent[Sol] {
  implicit val timeout: Timeout  = 5.seconds

  override def start(): Unit = agent ! StartAgent

  override def stop(): Unit = agent ! StopAgent

  override def numInvocations: Int = {
    Await.result(agent ? GetNumInvocations, 5.seconds)
      .asInstanceOf[Int]
  }
}
