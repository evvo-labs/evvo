package com.diatom.agent.func

import com.diatom.CreatorFunctionType

/**
  * A function that creates a new set of solutions.
  */
trait TCreatorFunc[Sol] extends TAgentFunc {
  def create: CreatorFunctionType[Sol]
}

case class CreatorFunc[Sol](create: CreatorFunctionType[Sol]) extends TCreatorFunc[Sol]
