package com.diatom.agent

import com.diatom.TPopulation
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
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
//      which are also available in their top-level under different names, unless they override?
//      should look into abstract classes and case classes, and figure out what to do
abstract class AAgent[Sol](protected val strategy: TAgentStrategy,
                           protected val population: TPopulation[Sol],
                           protected val name: String)
  extends TAgent[Sol] {

  protected val log = LoggerFactory.getLogger(this.getClass)
  var numInvocations: Int = 0

  // consider factoring this out into a separate component and using
  // composition instead of inheriting a thread field. This will reduce the
  // number of tests needed by centralizing
  private val thread = new Thread {
    override def run(): Unit = {
      var waitTime: Duration = strategy.waitTime(population.getInformation())
      try {
        while (!Thread.interrupted()) {
          try {
            numInvocations += 1
            step()
          } catch {
            case e: Exception => {
              e.printStackTrace()
              //              log.error(e.toString)
            }
          }
          Thread.sleep(waitTime.toMillis)

          if (numInvocations % 33 == 0) {
            val nextInformation = population.getInformation()
            waitTime = strategy.waitTime(nextInformation)
            //            log.debug(s"new waitTime: ${waitTime}")
          }
        }
      } catch {
        case e: InterruptedException => () // ignore Interrupted exception, this is expected
      }
    }
  }

  override def start(): Unit = {
    if (!thread.isAlive) {
      log.info(s"starting agent ${name}")
      thread.start()
    } else {
      log.warn(s"trying to start already start agent")
    }
  }

  override def stop(): Unit = {
    if (thread.isAlive) {
      log.info(s"${this}: stopping after ${numInvocations} invocations")
      thread.interrupt()
    } else {
      log.warn(s"trying to stop already stopped agent")
    }
  }

  /**
    * Performs one operation on the population.
    */
  protected def step(): Unit
}