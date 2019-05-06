package com.diatom.island.population

import com.diatom.island.population.HashingStrategy.HashingStrategy

/**
  * Represents a solution scored by mutliple fitness functions.
  */
trait TScored[Sol] {
  /**
    * Maps the name of fitness functions to the score of the solution with respect to them.
    */
  def score: Map[String, Double]


  def solution: Sol

  def hashStrategy: HashingStrategy.Value

  override def hashCode(): Int = hashStrategy match {
    case HashingStrategy.ON_SCORES => this.score.hashCode()
    case HashingStrategy.ON_SOLUTIONS => this.solution.hashCode()
  }

  override def equals(obj: Any): Boolean = obj match {
    case that: TScored[Sol] =>
      hashStrategy match {
        case HashingStrategy.ON_SCORES => this.score.equals(that.score)
        case HashingStrategy.ON_SOLUTIONS => this.solution.equals(that.solution)
      }
    case _ => false
  }
}

object HashingStrategy extends Enumeration {
  type HashingStrategy = Value
  val ON_SOLUTIONS, ON_SCORES = Value
}

case class Scored[Sol](score: Map[String, Double],
                       solution: Sol,
                       hashStrategy: HashingStrategy = HashingStrategy.ON_SCORES)
  extends TScored[Sol]