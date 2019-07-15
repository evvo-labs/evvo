package io.evvo

import io.evvo.agent.defaults.DeleteDominated
import io.evvo.agent.{CreatorFunction, MutatorFunction}
import io.evvo.island.population.{Maximize, Objective}
import io.evvo.island.{EvvoIslandBuilder, LocalIslandManager, StopAfter}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class LocalIslandManagerTest extends WordSpec with Matchers {

  "LocalIslandManager" should {
    "Be able to optimize problems" in {
      type Solution = String

      val startV = new Objective[Solution]("startV", Maximize) {
        override protected def objective(sol: Solution): Double = {
          sol.drop(1).dropRight(1).takeWhile(_ == 'v').length
        }
      }

      val endV = new Objective[Solution]("endV", Maximize) {
        override protected def objective(sol: Solution): Double = {
          sol.drop(1).dropRight(1).reverse.takeWhile(_ == 'v').length
        }
      }

      val creator = new CreatorFunction[Solution]("create") {
        override def create(): Iterable[Solution] = Vector("evvo")
      }

      val mutator = new MutatorFunction[Solution]("mutate") {
        override def mutate(sol: Solution): Solution = {
          val (e, rest) = sol.splitAt(1)
          e + util.Random.alphanumeric.head.toString + rest
        }
      }

      val builder = EvvoIslandBuilder[Solution]()
        .addObjective(startV)
        .addObjective(endV)
        .addCreator(creator)
        .addModifier(mutator)
        .addDeletor(DeleteDominated[Solution]())

      val manager = new LocalIslandManager(10, builder)

      manager.runBlocking(StopAfter(1000.millis))

      val pareto = manager.currentParetoFrontier()

      // The Pareto Frontier must have a solution with startV > 4 and endV > 4
      assert(pareto.solutions.exists(s =>
        s.score(("startV", Maximize)) > 4 && s.score(("endV", Maximize)) > 4))
    }
  }

}
