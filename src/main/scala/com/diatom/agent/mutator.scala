package com.diatom.agent

import akka.event.LoggingAdapter
import com.diatom.island.population.TPopulation
import scala.concurrent.duration._

trait TMutatorAgent[Sol] extends TAgent[Sol]

case class MutatorAgent[Sol](mutate: TMutatorFunc[Sol],
                             population: TPopulation[Sol],
                             strategy: TAgentStrategy = MutatorAgentDefaultStrategy())
                            (implicit val logger: LoggingAdapter)
  extends AAgent[Sol](strategy, population, mutate.name)(logger) with TMutatorAgent[Sol] {


  def step(): Unit = {
    //TODO validate size of input set
    val in = population.getSolutions(mutate.numInputs)
    val out = mutate.mutate(in)
    population.addSolutions(out)

  }
}

case class MutatorAgentDefaultStrategy() extends TAgentStrategy {
  // if there are too many solutions, give the deletor chance to work
  // TODO this is bad. fix it.
  override def waitTime(populationInformation: TPopulationInformation): Duration = {
    if (populationInformation.numSolutions > 1000) {
      100.millis
    } else {
      0.millis
    }
  }
}
