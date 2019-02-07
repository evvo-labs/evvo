package com.diatom.agent.func

import com.diatom.{TScored}

trait TMutatorFunc[Sol] extends TAgentFunc {
  def mutate(parents: Set[TScored[Sol]]): Set[Sol]

  def numInputs: Int
}
