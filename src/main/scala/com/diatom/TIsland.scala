package com.diatom

import com.diatom.island.TTerminationCriteria

/**
  * An evolutionary process with a set of actors that all act upon one set of solutions.
  */
trait TIsland[Solution] {

  def run(terminationCriteria: TTerminationCriteria): TParetoFrontier[Solution]

  /**
    * Kills the island (including all of its associated actors) and loses all state.
    */
  def forceKill(): Unit
}
