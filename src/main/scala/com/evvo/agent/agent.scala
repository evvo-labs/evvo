package com.evvo.agent

import akka.event.LoggingAdapter
import com.evvo.island.population.Population

import scala.concurrent.duration._

/**
  * The parent trait for all types of evolutionary agents.
  */
trait Agent[Sol] {
  def start(): Unit

  def stop(): Unit

  protected def numInvocations: Int
}

/**
  * Common behavior for all agents. Orchestrates the main loop, decides how often to run, etc.
  * All that is left to the subclasses is the actual content of the main loop.
  * @param population The population to add solutions to.
  * @param strategy Tells the CreatorAgent how often to run.
  * @param logger Where logs are written to. Not that this must extend the Akka
  *               [[akka.event.LoggingAdapter]] interface, not a more common logback
  *               or slf4s interface.
  * @param name The name of this agent. Used for logging.
  */
abstract class AAgent[Sol](private val strategy: AgentStrategy,
                           private val population: Population[Sol],
                           protected val name: String)
                          (private implicit val logger: LoggingAdapter)
  extends Agent[Sol] {

  /** The number of  times this agent has run its function. */
  protected var numInvocations: Int = 0

  // consider factoring this out into a separate component and using
  // composition instead of inheriting a thread field. This will reduce the
  // number of tests needed by centralizing
  private val thread = new Thread {
    override def run(): Unit = {
      var waitTime: Duration = strategy.waitTime(population.getInformation())
      logger.debug(s"${name}: Waiting for ${waitTime}")
      // The whole main loop in is in a try/catch block so we can log on exit, particularly
      // if there was an uncaught exception or a ThreadInterruptedException.
      try {
        while (!Thread.interrupted()) {
          // Each step in the main loop just calls the `step()` function, which subclasses override,
          // and increases the number of invocations, logging any exceptions but continuing to run.
          // Because the step() method tends to be stochastic, throwing an error this time doesn't
          // mean an error will be thrown next time, so we persist.
          try {
            numInvocations += 1
            step()
          } catch {
            case e: Exception => {
              logger.warning(
                f"${this}: Agent ${name} encountered an exception during a step, " +
                  f"stack trace: ${e.getStackTrace.mkString("\n")}")
            }
          }
          Thread.sleep(waitTime.toMillis)

          // this is arbitrary
          if (numInvocations % 33 == 0) {
            val nextInformation = population.getInformation()
            waitTime = strategy.waitTime(nextInformation)
            logger.debug(s"${name}: Waiting for ${waitTime}")
          }
          if (numInvocations % 100 == 0) {
            logger.debug(s"${this} hit ${numInvocations} invocations")
          }
        }
        logger.debug(f"${this}-${name}: Interrupted during while loop, terminating gracefully.")
      } catch {
        // if interrupted, silently exit. This thread is interrupted only when AAgent.stop() is
        // called, so there's nothing more to do.
        case e: InterruptedException =>
          logger.info(f"${this}-${name}: Interrupted during sleep, terminating gracefully.")
        case e: Exception =>
          logger.error(f"${this}-${name}: Unexpected exception ${e}, stopping thread. " +
            f"Stack trace: ${e.getStackTrace.mkString("\n")}")
      }
      logger.info(f"${this}-${name}: finished running")
    }
  }

  override def start(): Unit = {
    if (!thread.isAlive) {
      logger.info(s"${this}-${name}: Starting agent")
      thread.start()
    } else {
      logger.warning(s"${this}-${name}: trying to start already started agent")
    }
  }

  override def stop(): Unit = {
    if (thread.isAlive) {
      logger.info(s"${this}-${name}: stopping after ${numInvocations} invocations")
      thread.interrupt()
    } else {
      logger.warning(s"${this}-${name}: trying to stop already stopped agent, " +
        s"with ${numInvocations} invocations")
    }
  }

  /**
    * Performs one operation on the population. Once the agent is started, this method will be
    * called repeatedly until the agent is stopped.
    */
  protected def step(): Unit

  override def toString: String = f"Agent[$name]"
}
