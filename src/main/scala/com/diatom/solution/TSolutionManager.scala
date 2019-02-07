package com.diatom.solution

/**
  * Holds functions which operate on the solution, to provide additional capabilities
  * on solution types, which can be flat data.
  * @tparam Solution the type of solution it manages.
  */
trait TSolutionManager[Solution] {
  def stringify(s: Solution): String = s.toString
}
