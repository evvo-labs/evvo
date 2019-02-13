package com.diatom.agent

trait TDeletorAgent[Sol] extends TAgent[Sol] {
}

class DeletorAgent[Sol] extends TDeletorAgent[Sol] {



  override def start(): Unit = ???

  override def stop(): Unit = ???
}