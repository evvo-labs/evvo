package com.diatom.island.population

import com.diatom.ObjectiveFunctionType

/**
  * A real valued objective.
  */
trait TObjective[Sol] {

  /**
    * The score, to be minimized.
    * param: the solution to score
    * @return the score, according to this objective
    */
  def score:  ObjectiveFunctionType[Sol]

  def name: String

  /**
    * @return How many significant figures to keep.
    */
  def precision: Int

  /**
    * @return the direction to optimize in - either Minimize or Maximize
    */
  def optimizationDirection: OptimizationDirection

  override def toString: String = s"Fitness[${this.name}]"
}

case class Objective[Sol](private val objective: ObjectiveFunctionType[Sol],
                          name: String,
                          optimizationDirection: OptimizationDirection,
                          precision: Int = 3)
  extends TObjective[Sol] {
  override def score: ObjectiveFunctionType[Sol] = sol => {
    val roundingMultiple = math.pow(10, precision)
    math.round(objective(sol) * roundingMultiple) / roundingMultiple
  }
}

sealed trait OptimizationDirection
object Minimize extends OptimizationDirection
object Maximize extends OptimizationDirection
