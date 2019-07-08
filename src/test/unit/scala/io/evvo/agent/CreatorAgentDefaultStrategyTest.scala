package io.evvo.agent

import io.evvo.agent.{CreatorAgentDefaultStrategy, PopulationInformation}
import org.scalatest.WordSpec

class CreatorAgentDefaultStrategyTest extends WordSpec {
  "CreatorAgentDefaultStrategy" should {
    "wait longer if the population is size 5000 than if pop is size 100" in {
      val strat = CreatorAgentDefaultStrategy()
      val waitTimeSmallPop = strat.waitTime(PopulationInformation(100))
      val waitTimeLargePop = strat.waitTime(PopulationInformation(5000))
      assert(waitTimeSmallPop < waitTimeLargePop)
    }
  }
}
