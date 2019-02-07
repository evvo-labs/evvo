package com.diatom.agent

import com.diatom.CreatorFunc

trait TCreatorAgent[Sol] extends TAgent[Sol] {

}

case class CreatorAgent[Sol](creatorFunc: CreatorFunc[Sol]) extends TCreatorAgent[Sol] {
  override def start(): Unit = ???

  override def stop(): Unit = ???
}
