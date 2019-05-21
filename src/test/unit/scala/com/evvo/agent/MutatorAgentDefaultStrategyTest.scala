package com.evvo.agent

import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._

class MutatorAgentDefaultStrategyTest extends WordSpec with Matchers {
  "MutatorAgentDefaultStrategy" should {
    val smallPop = PopulationInformation(1000)
    val largePop = PopulationInformation(10000)

    // commented out because it's possibly valid in the future
    //    "wait more if there are too many solutions" in {
    //      val strat = MutatorAgentDefaultStrategy()
    //      assert(strat.waitTime(smallPop) < strat.waitTime(largePop))
    //    }
    "always wait 0ms" in {
      assert(MutatorAgentDefaultStrategy().waitTime(smallPop) == 0.millis)
    }
  }
}
