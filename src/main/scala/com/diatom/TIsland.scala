package com.diatom

/**
  * An evolutionary process with a set of actors that all act upon one set of solutions.
  */
trait TIsland[Solution] {

  def run(): TParetoFrontier[Solution]

  /**
    * Kills the island (including all of its associated actors) and loses all state.
    */
  def forceKill(): Unit

  /**
    * A set of criteria specifying when an TIsland should be terminated.
    */
  trait TTerminationCriteria {

    def time(time: Long): Boolean = false

    def paretoFrontier(paretoFrontier: TParetoFrontier[Solution]): Boolean = false

  }
}
