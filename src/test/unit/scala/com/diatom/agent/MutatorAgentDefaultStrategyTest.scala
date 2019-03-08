package com.diatom.agent

import org.scalatest.{Matchers, WordSpec}

class MutatorAgentDefaultStrategyTest extends WordSpec with Matchers {
  "MutatorAgentDefaultStrategy" should {
    "wait more if there are too many solutions" in {
      val strat = MutatorAgentDefaultStrategy()
      val smallPop = PopulationInformation(1000)
      val largePop = PopulationInformation(10000)
      assert(strat.waitTime(smallPop) < strat.waitTime(largePop))
    }
  }
}
