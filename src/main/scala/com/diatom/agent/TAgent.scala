package com.diatom.agent

/**
  * The parent trait for all types of evolutionary agents.
  */
trait TAgent[Solution] {
  def start(): Unit
  def stop(): Unit
}

abstract class AAgent[Solution] extends TAgent[Solution] {

}
