package com.evvo.island

import com.evvo.agent.defaults.{Bitflipper, Bitstring, BitstringGenerator, DeleteDominated}
import com.evvo.island.population.{Maximize, Objective, ParetoFrontier, Scored}
import org.scalatest.{Matchers, WordSpec}

class EvvoIslandBuilderTest extends WordSpec with Matchers {
  "EvvoIslandBuilders" should {
    "be able to produce LocalIslands" in {

      val builder = EvvoIsland.builder[Bitstring]()
        .addObjective(new Objective[Bitstring]("Three", Maximize) {
          override protected def objective(sol: Bitstring): Double = 3d
        })
        .addCreator(BitstringGenerator(length=16))
        .addMutator(Bitflipper())
        .addDeletor(DeleteDominated())

      builder.buildLocalEvvo()
    }

    "allow adding items in any order" in {
      val builder = EvvoIsland.builder[Bitstring]()
        .addDeletor(DeleteDominated())
        .addCreator(BitstringGenerator(length=16))
        .addObjective(new Objective[Bitstring]("Three", Maximize) {
          override protected def objective(sol: Bitstring): Double = 3d
        })
        .addMutator(Bitflipper())
    }

    "require at least one Objective, Creator, Mutator, Deletor" in {
      // Leaving this commented out: it's a compile time error. But I'm leaving it here
      // to inform people in the future.
//       val builder = EvvoIsland.builder[Bitstring]().buildLocalEvvo()
    }
  }

}
