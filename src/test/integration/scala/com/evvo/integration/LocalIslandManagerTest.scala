package com.evvo.integration

import com.evvo.agent.defaults.DeleteDominated
import com.evvo.island.population.{Maximize, Objective}
import com.evvo.island.{EvvoIslandBuilder, LocalIslandManager, StopAfter}
import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._

class LocalIslandManagerTest extends WordSpec with Matchers {

  "LocalIslandManager" should {
    "Be able to optimize problems" in {
//
//      type Solution = String
//
//      val builder = EvvoIslandBuilder[Solution]()
//        .addObjective(Objective[Solution](
//          s => s.drop(1).dropRight(1).takeWhile(_ == 'v').length,
//          "startV",
//          Maximize))
//        .addObjective(Objective[Solution](
//          s => s.drop(1).dropRight(1).reverse.takeWhile(_ == 'v').length,
//          "endV",
//          Maximize))
//        .addCreatorFromFunction(() => Set("evvo"))
//        .addMutatorFromFunction((sols) => sols.map(s => {
//          val (e, rest) = s.solution.splitAt(1)
//          e + util.Random.alphanumeric.head.toString + rest
//        }))
//        .addDeletor(DeleteDominated[Solution]())
//
//      val manager = new LocalIslandManager(7, builder)
//
//      manager.runBlocking(StopAfter(1.second))
//
//      val pareto = manager.currentParetoFrontier()
//      assert(pareto.solutions.exists(s =>
//        s.score(("startV", Maximize)) > 10 && s.score(("endV", Maximize)) > 10))
    }
  }

}
