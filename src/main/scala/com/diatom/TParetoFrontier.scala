package com.diatom

/**
  * Represents a set of solutions that are non-dominated.
  */
trait TParetoFrontier[Solution] {
  def solutions: Set[Solution]
}
