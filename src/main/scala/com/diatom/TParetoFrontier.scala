package com.diatom

/**
  * Represents a set of solutions that are non-dominated.
  */
trait TParetoFrontier[Sol] {
  def solutions: Set[Sol]
}

case class ParetoFrontier[Sol](solutions: Set[Sol]) extends TParetoFrontier[Sol]