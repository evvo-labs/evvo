package com.diatom

/**
  * Represents a solution scored by mutliple fitness functions.
  */
trait TScored[Sol] {
  /**
    * Maps the name of fitness functions to the score of the solution with respect to them.
    */
  def score: Map[String, Double]

  def solution: Sol

  /**
    * Does this solution dominate that one?
    * If the other solution has a different set of fitness functions than this one,
    * always returns false.
    *
    * @param that a scored solution.
    * @return whether this solution dominates that one
    */
  def dominates(that: TScored[Sol]): Boolean = ???
}
