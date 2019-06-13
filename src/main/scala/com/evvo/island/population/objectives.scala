package com.evvo.island.population


/**
  * @param name                  the name of this objective
  * @param optimizationDirection oneof `Minimize` or `Maximize`
  * @param precision             how many decimal points to round to. For example, if given 1,
  *                              .015 and .018 will be considered equal.
  */
abstract class Objective[Sol](
                          val name: String,
                          val optimizationDirection: OptimizationDirection,
                          precision: Int = 3)  // scalastyle:ignore magic.number
  extends Serializable {
  /** The underlying function: takes a `Sol` and scores it. */
  protected def objective(sol: Sol): Double

  /** Scores the given solution, rounding to the precision specified. */
  def score(sol: Sol): Double = {
    val roundingMultiple = math.pow(10, precision) // scalastyle:ignore magic.number
    math.round(objective(sol) * roundingMultiple) / roundingMultiple
  }
}

sealed trait OptimizationDirection
case object Minimize extends OptimizationDirection
case object Maximize extends OptimizationDirection
