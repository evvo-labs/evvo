package io.evvo.island

import io.evvo.agent.{CreatorFunction, ModifierFunction}
import io.evvo.builtin.bitstrings.Bitstring
import io.evvo.builtin.deletors.DeleteDominated
import io.evvo.island.population._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class IslandManagerTest extends WordSpec with Matchers {
  "A Local Island Manager" should {
    "Use the provided network topology to register islands with each other" in {

      case object NoCreator extends CreatorFunction[Bitstring]("no") {
        override def create(): Iterable[Bitstring] = Seq()
      }
      case object NoModifier extends ModifierFunction[Bitstring]("No") {
        override def modify(sols: IndexedSeq[Scored[Bitstring]]): Iterable[Bitstring] = Seq()
      }
      case object OneMax extends Objective[Bitstring]("OneMax", Maximize) {
        override protected def objective(sol: Bitstring): Double = sol.count(identity)
      }

      // Make sure they aren't creating any solutions
      val builder = EvvoIslandBuilder[Bitstring]()
        .addCreator(NoCreator)
        .addModifier(NoModifier)
        .addDeletor(DeleteDominated())
        .addObjective(OneMax)
        .withEmigrationStrategy(WholeParetoFrontierEmigrationStrategy())
        .withEmigrationTargetStrategy(SendToAllEmigrationTargetStrategy())

      // Expression blocks so we can resuse the names island1, etc.
      val _ = {
        val island1 = builder.buildLocalEvvo()
        val island2 = builder.buildLocalEvvo()
        val island3 = builder.buildLocalEvvo()
        // simply constructing the manager registers the islands with each other
        val mgr = new IslandManager[Bitstring](
          Seq(island1, island2, island3),
          FullyConnectedNetworkTopology())
        island1.immigrate(Seq(Scored(Map(), Seq(true))))

        mgr.runBlocking(StopAfter(1.second))
        island2.currentParetoFrontier().solutions should have size 1
        island3.currentParetoFrontier().solutions should have size 1
      }

      case object NoTopology extends NetworkTopology {
        override def configure(numIslands: Int): Seq[Connection] = Seq()
      }

      val _ = {
        val island1 = builder.buildLocalEvvo()
        val island2 = builder.buildLocalEvvo()
        val mgr =
          new IslandManager[Bitstring](Seq(island1, island2), NoTopology)
        island1.immigrate(Seq(Scored(Map(), Seq(true))))
        mgr.runBlocking(StopAfter(1.second))

        island2.currentParetoFrontier().solutions should have size 0
      }
    }
  }
}
