package com.diatom.agent

import akka.event.LoggingAdapter
import com.diatom.island.population.TPopulation
import scala.concurrent.duration._

/**
  * The parent trait for all types of evolutionary agents.
  */
trait TAgent[Sol] {
  def start(): Unit

  def stop(): Unit

  def numInvocations: Int
}

abstract class AAgent[Sol](private val strategy: TAgentStrategy,
                           private val population: TPopulation[Sol],
                           private val name: String)
                          (private implicit val logger: LoggingAdapter)
  extends TAgent[Sol] {

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

          // TODO oh god, this is arbitrary. Now that populations are running locally and this
          // won't require a blocking Akka actor call, maybe we can recompute every time, or
          // every second?
          if (numInvocations % 33 == 0) {
            val nextInformation = population.getInformation()
            waitTime = strategy.waitTime(nextInformation)
            //            log.debug(s"new waitTime: ${waitTime}")
          }
        }
      } catch {
        case e: InterruptedException => return
      }
    }
  }

  override def start(): Unit = {
    if (!thread.isAlive) {
      logger.info(s"starting agent ${name}")
      thread.start()
    } else {
      logger.warning(s"trying to start already start agent")
    }
  }

  override def stop(): Unit = {
    if (thread.isAlive) {
      logger.info(s"${this}: stopping after ${numInvocations} invocations")
      thread.interrupt()
    } else {
      logger.warning(s"trying to stop already stopped agent")
    }
  }

  /**
    * Performs one operation on the population.
    */
  protected def step(): Unit
}