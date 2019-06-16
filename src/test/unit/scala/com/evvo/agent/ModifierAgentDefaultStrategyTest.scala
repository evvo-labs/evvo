package com.evvo.agent

import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._

class ModifierAgentDefaultStrategyTest extends WordSpec with Matchers {
  "MutatorAgentDefaultStrategy" should {
    val smallPop = PopulationInformation(1000)
    val largePop = PopulationInformation(10000)

    "always wait 0ms" in {
      assert(ModifierAgentDefaultStrategy().waitTime(smallPop) == 0.millis)
      assert(ModifierAgentDefaultStrategy().waitTime(largePop) == 0.millis)
    }
  }
}
