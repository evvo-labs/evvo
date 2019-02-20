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

  private val thread = new Thread {
    override def run: Unit = {
      Try {
        while (!Thread.interrupted()) {
          Thread.sleep(500) // TODO: This represents an initial strategy, replace it.
          step()
        }
      }
    }
  }

  override def start(): Unit = {
    thread.start()
  }

  override def stop(): Unit = {
    thread.interrupt()
  }

  /**
    * Performs one operation on the population.
    */
  protected def step(): Unit
}
