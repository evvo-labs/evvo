package com.diatom.agent

import com.diatom.agent.func.TDeletorFunc

trait TDeletorAgent[Sol] extends TAgent[Sol] {

}

case class DeletorAgent[Sol](delete: TDeletorFunc[Sol]) extends TDeletorAgent[Sol] {

  override def start(): Unit = ???

  override def stop(): Unit = ???
}

