package com.diatom.agent

import com.diatom.FitnessFunctionType

/**
  * A real valued objective.
  */
trait TFitnessFunc[Sol] {

  /**
    * The score, to be minimized.
    * param: the solution to score
    * @return the score, according to this objective
    */
  def score: Sol => Double

  def name: String

  override def toString: String = s"Fitness[${this.name}]"
}

case class FitnessFunc[Sol](score: FitnessFunctionType[Sol], name: String) extends TFitnessFunc[Sol]
