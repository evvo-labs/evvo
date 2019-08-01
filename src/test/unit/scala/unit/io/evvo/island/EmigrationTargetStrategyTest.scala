package unit.io.evvo.island

import io.evvo.island.{RoundRobinEmigrationTargetStrategy, SendToAllEmigrationTargetStrategy}
import org.scalatest.{Matchers, WordSpec}

class EmigrationTargetStrategyTest extends WordSpec with Matchers {
  "Round Robin emigration target strategy" should {
    "round robin" in {
      val rr = RoundRobinEmigrationTargetStrategy()
      rr.chooseTargets(3) shouldBe Seq(0)
      rr.chooseTargets(3) shouldBe Seq(1)
      rr.chooseTargets(3) shouldBe Seq(2)
    }
  }

  "SendToAll emigration target strategy" should {
    "send to all" in {
      SendToAllEmigrationTargetStrategy().chooseTargets(4) shouldBe Seq(0, 1, 2, 3)
    }
  }
}
