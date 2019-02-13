package com.diatom.agent

/**
  * A real valued objective.
  */
trait TFitnessFunc[Sol] {

  /**
    * The score, to be maximized.
    * param: the solution to score
    * @return the score, according to this objective
    */
  def score: Sol => Double
}

case class FitnessFunc[Sol](score: Sol => Double) extends TFitnessFunc[Sol]
