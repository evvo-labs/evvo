package com.diatom.agent

import com.diatom.agent.func.TDeletorFunc
import com.diatom.population.TPopulation

trait TDeletorAgent[Sol] extends TAgent[Sol] {

}

case class DeletorAgent[Sol](delete: TDeletorFunc[Sol], pop: TPopulation[Sol]) extends TDeletorAgent[Sol] {

  override def start(): Unit = ???

  override def stop(): Unit = ???
}

