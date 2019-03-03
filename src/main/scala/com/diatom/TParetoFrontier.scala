package com.diatom

/**
  * Represents a set of solutions that are non-dominated.
  */
trait TParetoFrontier[Sol] {
  def solutions: Set[TScored[Sol]]
}

case class ParetoFrontier[Sol](solutions: Set[TScored[Sol]]) extends TParetoFrontier[Sol]