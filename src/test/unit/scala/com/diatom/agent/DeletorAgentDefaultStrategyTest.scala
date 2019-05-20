package com.diatom.agent

import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._

class DeletorAgentDefaultStrategyTest extends WordSpec with Matchers {
  "DeletorAgentDefaultStrategy" should {
    val smallPop = PopulationInformation(1000)
    val largePop = PopulationInformation(10000)

//    "wait less if the population is size 5000 than if pop is size 100" in {
//      val strat = DeletorAgentDefaultStrategy()
//      val waitTimeSmallPop = strat.waitTime(PopulationInformation(100))
//      val waitTimeLargePop = strat.waitTime(PopulationInformation(5000))
//      assert(waitTimeSmallPop > waitTimeLargePop)
//    }

    "always wait 0ms" in {
      assert(DeletorAgentDefaultStrategy().waitTime(smallPop) == 0.millis)
    }
  }
}
