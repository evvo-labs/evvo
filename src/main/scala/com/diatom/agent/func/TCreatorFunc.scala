package com.diatom.agent.func

trait TCreatorFunc[Sol] extends TAgentFunc {
  def create(): Set[Sol]
}
