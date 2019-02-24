package com.diatom.agent

import com.diatom.population.TPopulation

import scala.util.Try

/**
  * The parent trait for all types of evolutionary agents.
  */
trait TAgent[Sol] {
  def start(): Unit
  def stop(): Unit
}

abstract class AAgent[Sol](pop: TPopulation[Sol]) extends TAgent[Sol] {
  // consider factoring this out into a separate component and using
  // composition instead of inheriting a thread field. This will reduce the
  // number of tests needed by centralizing
  private val thread = new Thread {
    override def run(): Unit = {
      Try {
        while (!Thread.interrupted()) {
          step()
          Thread.sleep(500) // TODO: This represents an initial strategy, replace it.
        }
      }
    }
  }

  override def start(): Unit = {
    println(thread.isAlive)
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
