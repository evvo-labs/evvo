package com.evvo.agent

import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._

class DeletorAgentDefaultStrategyTest extends WordSpec with Matchers {
  "DeletorAgentDefaultStrategy" should {
    val smallPop = PopulationInformation(1000)
    val largePop = PopulationInformation(10000)

    "always wait 0ms" in {
      assert(DeletorAgentDefaultStrategy().waitTime(smallPop) == 0.millis)
      assert(DeletorAgentDefaultStrategy().waitTime(largePop) == 0.millis)
    }
  }
}
