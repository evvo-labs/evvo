package com.diatom

/**
  * Manages the agents acting upon the solution set in one island.
  */
trait TManager[Solution] {

  def start(): Unit

  def stop(): Unit

}
