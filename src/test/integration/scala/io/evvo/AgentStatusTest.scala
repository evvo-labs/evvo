///** This file is in integration tests because it runs the entire island to check for performance
//  * metrics.
//  */
//package integration.scala.io.evvo
//
//import io.evvo.LocalEvvoTestFixtures.{NumInversions, ReverseListCreator, SwapTwoElementsModifier}
//import io.evvo.agent.{CreatorFunction, DeletorFunction, MutatorFunction}
//import io.evvo.builtin.deletors.DeleteWorstHalfByRandomObjective
//import io.evvo.island.{EvolutionaryProcess, EvvoIsland, StopAfter}
//import io.evvo.tags.{Performance, Slow}
//import org.scalatest.{Matchers, WordSpec}
//
//import scala.concurrent.duration._
//
//class AgentStatusTest extends WordSpec with Matchers {
//
//  "LocalIslandManager" should {
//    "Be able to return agents" taggedAs (Performance, Slow) in {
//      type Solution = List[Int]
//      val creator: CreatorFunction[Solution] = new ReverseListCreator(10)
//      val modifier: MutatorFunction[Solution] = new SwapTwoElementsModifier()
//      val deletor: DeletorFunction[Solution] = DeleteWorstHalfByRandomObjective()
//
//      new NumInversions()
//      val evvo: EvolutionaryProcess[Solution] = EvvoIsland[Solution]()
//        .addCreator(creator)
//        .addModifier(modifier)
//        .addDeletor(deletor)
//        .addObjective(new NumInversions())
//        .build()
//
//      assert(evvo.agentStatuses().forall(_.numInvocations == 0))
//      evvo.runBlocking(StopAfter(300.millis))
//      assert(evvo.agentStatuses().forall(_.numInvocations > 0))
//    }
//  }
//}
