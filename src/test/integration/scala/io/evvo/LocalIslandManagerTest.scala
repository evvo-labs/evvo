package io.evvo

import io.evvo.agent.{CreatorFunction, MutatorFunction}
import io.evvo.builtin.bitstrings.{Bitflipper, Bitstring, BitstringGenerator}
import io.evvo.builtin.deletors.DeleteDominated
import io.evvo.island.population.{FullyConnectedNetworkTopology, Maximize, Objective}
import io.evvo.island.{EvvoIslandBuilder, LocalIslandManager, StopAfter}
import io.evvo.tags.Slow
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class LocalIslandManagerTest extends WordSpec with Matchers {

  "LocalIslandManager" should {
    "Be able to optimize problems" taggedAs Slow in {
      // This is a general-purpose, high level test to ensure that everything works on
      // a basic level. It makes sure that optimization actually happens, with multiple islands.
      // If this test starts taking 10 seconds, you know that parallelism has stopped happening
      // locally.
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

      val manager = new LocalIslandManager(10, builder, FullyConnectedNetworkTopology())

      manager.runBlocking(StopAfter(1000.millis))

      val pareto = manager.currentParetoFrontier()

      // The Pareto Frontier must have a solution with startV > 4 and endV > 4
      assert(
        pareto.solutions
          .exists(s => s.score(("startV", Maximize)) > 4 && s.score(("endV", Maximize)) > 4)
      )
    }
  }
}
