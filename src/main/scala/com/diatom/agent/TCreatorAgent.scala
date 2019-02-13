package com.diatom.agent

import com.diatom.CreatorFunctionType

trait TCreatorAgent[Sol] extends TAgent[Sol] {

}

case class CreatorAgent[Sol](creatorFunc: CreatorFunctionType[Sol]) extends TCreatorAgent[Sol] {
  override def start(): Unit = ???

  override def stop(): Unit = ???
}
