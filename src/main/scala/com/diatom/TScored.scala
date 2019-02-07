package com.diatom

import com.diatom.agent.TFitnessFunction

trait TScored[Sol] {
  def score: Map[TFitnessFunction[Sol], Double]

  /**
    * Does this solution dominate that one?
    * If the other solution has a different set of fitness functions than this one,
    * always returns false.
    *
    * @param that a scored solution.
    * @return whether this solution dominates that one
    */
  //TODO implement this
  def dominates(that: TScored[Sol]): Boolean = ???
}
