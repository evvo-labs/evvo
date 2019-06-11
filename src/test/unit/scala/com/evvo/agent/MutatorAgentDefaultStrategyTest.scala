package com.evvo.agent

import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._

class MutatorAgentDefaultStrategyTest extends WordSpec with Matchers {
  "MutatorAgentDefaultStrategy" should {
    val smallPop = PopulationInformation(1000)
    val largePop = PopulationInformation(10000)

    "always wait 0ms" in {
      assert(MutatorAgentDefaultStrategy().waitTime(smallPop) == 0.millis)
      assert(MutatorAgentDefaultStrategy().waitTime(largePop) == 0.millis)
    }
  }
}
