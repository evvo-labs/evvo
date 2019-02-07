package com.diatom.agent

trait TFitnessFunction[Solution] {
  /**
    * The score, to be maximized.
    * @param s the solution to score
    * @return the score, according to this objective
    */
  def score(s: Solution): Double
}
