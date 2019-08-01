package io.evvo.island

import io.evvo.builtin.bitstrings.{Bitflipper, Bitstring, BitstringGenerator}
import io.evvo.builtin.deletors.DeleteDominated
import io.evvo.island.population._
import org.scalatest.{Matchers, WordSpec}

class IslandManagerTest extends WordSpec with Matchers {
  "A Local Island Manager" should {
    "Use the provided network topology to register islands with each other" in {
      case object OneMax extends Objective[Bitstring]("OneMax", Maximize) {
        override protected def objective(sol: Bitstring): Double = sol.count(identity)
      }

      // Not actually relevant for this test, just needs to compile.
      val builder = EvvoIslandBuilder[Bitstring]()
        .addCreator(BitstringGenerator(8))
        .addModifier(Bitflipper())
        .addDeletor(DeleteDominated())
        .addObjective(OneMax)

      // Expression blocks so we can resuse the names island1, etc.
      val _ = {
        val island1 = builder.buildLocalEvvo()
        val island2 = builder.buildLocalEvvo()
        val island3 = builder.buildLocalEvvo()
        // simply constructing the manager registers the islands with each other
        val _ = new IslandManager[Bitstring](
          Seq(island1, island2, island3),
          FullyConnectedNetworkTopology)
        island1.immigrate(Seq(Scored(Map(), Seq(true))))

        island2.currentParetoFrontier().solutions shouldBe Set(Seq(true))
        island3.currentParetoFrontier().solutions shouldBe Set(Seq(true))
      }

      val _ = {
        val island1 = builder.buildLocalEvvo()
        val island2 = builder.buildLocalEvvo()
        val island3 = builder.buildLocalEvvo()
        val _ =
          new IslandManager[Bitstring](Seq(island1, island2, island3), RingNetworkTopology)
        island1.immigrate(Seq(Scored(Map(), Seq(true))))

        // In a ring network, only one or the other is connected.
        if (island2.currentParetoFrontier().solutions.isEmpty) {
          island3.currentParetoFrontier().solutions shouldBe Set(Seq(true))
        } else {
          island3.currentParetoFrontier().solutions should be(empty)
        }
      }

    }
  }
}
