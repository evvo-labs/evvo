package com.diatom.agent

import com.diatom.population.TPopulation

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
                           protected val population: TPopulation[Sol])
  extends TAgent[Sol] {
  var numInvocations: Int = 0

  // consider factoring this out into a separate component and using
  // composition instead of inheriting a thread field. This will reduce the
  // number of tests needed by centralizing
  private val thread = new Thread {
    override def run(): Unit = {
      var waitTime: Duration = strategy.waitTime(population.getInformation())
      Try {
        while (!Thread.interrupted()) {
          try {
            numInvocations += 1
            step()
          } catch {
            case e: Exception => {
              //              log.error(e.toString)
            }
          }

          Thread.sleep(waitTime.toMillis)

          if (numInvocations % 33 == 0) {
            // TODO this is blocking, on population.getInformation(), can't be in main loop
            val nextInformation = population.getInformation()
            waitTime = strategy.waitTime(nextInformation)
            //            log.debug(s"new waitTime: ${waitTime}")
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
    println(s"${this}: stopping after ${numInvocations} invocations")
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