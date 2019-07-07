package com.evvo.island.population


/**
  * An objective is a goal to be maximized (or minimized) by the evolutionary process.
  *
  * @param name                  the name of this objective
  * @param optimizationDirection one of `Minimize` or `Maximize`
  * @param precision             how many decimal points to round to. For example, if given 1,
  *                              .015 and .018 will be considered equal.
  */
abstract class Objective[Sol](val name: String,
                              val optimizationDirection: OptimizationDirection,
                              precision: Int = 3)
  extends Serializable {
  /** The underlying function: takes a `Sol` and scores it. Protected because external callers
    * should be using `score` instead.
    *
    * @param sol The solution to score.
    * @return The score the given `Sol` earned.
    */
  protected def objective(sol: Sol): Double

  /** Scores the given solution, rounding to the precision specified. */
  def score(sol: Sol): Double = {
    val roundingMultiple = math.pow(10, precision) // scalastyle:ignore magic.number
    math.round(objective(sol) * roundingMultiple) / roundingMultiple
  }
}

/** An OptimizationDirection is either Minimize or Maximize. */
sealed trait OptimizationDirection
case object Minimize extends OptimizationDirection
case object Maximize extends OptimizationDirection
