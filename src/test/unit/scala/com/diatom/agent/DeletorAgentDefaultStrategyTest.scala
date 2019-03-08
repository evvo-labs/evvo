package com.diatom.agent

import org.scalatest.{Matchers, WordSpec}

class DeletorAgentDefaultStrategyTest extends WordSpec with Matchers {
  "DeletorAgentDefaultStrategy" should {
    "wait less if the population is size 5000 than if pop is size 100" in {
      val strat = DeletorAgentDefaultStrategy()
      val waitTimeSmallPop = strat.waitTime(PopulationInformation(100))
      val waitTimeLargePop = strat.waitTime(PopulationInformation(5000))
      assert(waitTimeSmallPop > waitTimeLargePop)
    }
  }
}
