package com.diatom.agent

import com.diatom.agent.func.TMutatorFunc

trait TMutatorAgent[Sol] extends TAgent[Sol] {

}

case class MutatorAgent[Sol](mutate: TMutatorFunc[Sol]) extends TMutatorAgent[Sol] {

  override def start(): Unit = ???

  override def stop(): Unit = ???

}
