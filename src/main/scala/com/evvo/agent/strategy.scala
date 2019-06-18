package com.evvo.agent

import scala.concurrent.duration._

/**
  * A strategy, for how often each agent should run its step of the mutation process.
  * Given some information, returns how long to wait between each invocation of the
  * agent's task.
  */
trait AgentStrategy {
  def waitTime(populationInformation: PopulationInformation): Duration
}

/**
  * Information about the population: meant to be used in communication
  * between the population and strategies of agents. Provides enough information
  * for agents to base their decisions on.
  */
case class PopulationInformation(numSolutions: Int)
