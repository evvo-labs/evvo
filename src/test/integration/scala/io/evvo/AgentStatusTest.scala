/** This file is in integration tests because it runs the entire island to check for performance
  * metrics.
  */
package integration.scala.io.evvo

import io.evvo.LocalEvvoTestFixtures.{
  NumInversions,
  ReverseListCreator,
  Solution,
  SwapTwoElementsModifier
}
import io.evvo.NullLogger
import io.evvo.agent.{CreatorFunction, DeletorFunction, MutatorFunction}
import io.evvo.builtin.deletors.DeleteWorstHalfByRandomObjective
import io.evvo.island.{EvolutionaryProcess, EvvoIslandBuilder, StopAfter}

import scala.concurrent.duration._
import io.evvo.tags.{Performance, Slow}
import org.scalatest.{Matchers, WordSpec}

class AgentStatusTest extends WordSpec with Matchers {

  implicit val log = NullLogger

  "LocalIslandManager" should {
    "Be able to return agents" taggedAs (Performance, Slow) in {
      type Solution = List[Int]
      val creator: CreatorFunction[Solution] = new ReverseListCreator(10)
      val modifier: MutatorFunction[Solution] = new SwapTwoElementsModifier()
      val deletor: DeletorFunction[Solution] = new DeleteWorstHalfByRandomObjective()

      new NumInversions()
      val evvo: EvolutionaryProcess[Solution] = EvvoIslandBuilder[Solution]()
        .addCreator(creator)
        .addModifier(modifier)
        .addDeletor(deletor)
        .addObjective(new NumInversions())
        .buildLocalEvvo()

      assert(evvo.agentStatuses().forall(_.numInvocations == 0))
      evvo.runBlocking(StopAfter(300.millis))
      assert(evvo.agentStatuses().forall(_.numInvocations > 0))
    }
  }
}
