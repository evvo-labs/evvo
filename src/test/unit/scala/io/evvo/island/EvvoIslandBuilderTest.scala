package io.evvo.island

import io.evvo.builtin.bitstrings.{Bitflipper, Bitstring, BitstringGenerator}
import io.evvo.builtin.deletors.DeleteDominated
import io.evvo.island.population.{Maximize, Objective}
import io.evvo.migration.closedborders.{DoNotRecord, DoNothingEmigrator, DoNothingImmigrator}
import org.scalatest.{Matchers, WordSpec}

class EvvoIslandBuilderTest extends WordSpec with Matchers {

  "EvvoIslandBuilders" should {
    val completeBuilder = EvvoIsland
      .builder[Bitstring]()
      .addObjective(new Objective[Bitstring]("Three", Maximize()) {
        override protected def objective(sol: Bitstring): Double = 3d
      })
      .addCreator(BitstringGenerator(length = 16))
      .addModifier(Bitflipper())
      .addDeletor(DeleteDominated())
      .withImmigrator(DoNothingImmigrator())
      .withImmigrationStrategy(ElitistImmigrationStrategy())
      .withEmigrator(DoNothingEmigrator())
      .withEmigrationStrategy(NoEmigrationEmigrationStrategy)
      .withParetoFrontierRecorder(DoNotRecord())
      .withLoggingStrategy(LogPopulationLoggingStrategy())

    "be able to produce EvvoIslands" in {
      completeBuilder.build()
    }

//    TODO: This will all make more sense once we have Local Immigrators/Emigrators.
//
//    "allow adding items in any order" in {
//      val builder = EvvoIsland
//        .builder[Bitstring]()
//        .addDeletor(DeleteDominated())
//        .addCreator(BitstringGenerator(length = 16))
//        .addObjective(new Objective[Bitstring]("Three", Maximize()) {
//          override protected def objective(sol: Bitstring): Double = 3d
//        })
//        .addModifier(Bitflipper())
//
//      builder.build()
//    }
//
//    "allow building without immigration strategy" in {
//      EvvoIsland
//        .builder[Bitstring]()
//        .addObjective(new Objective[Bitstring]("Three", Maximize()) {
//          override protected def objective(sol: Bitstring): Double = 3d
//        })
//        .addCreator(BitstringGenerator(length = 16))
//        .addModifier(Bitflipper())
//        .addDeletor(DeleteDominated())
//        .build()
//    }
//
//    "allow building without creator" in {
//      EvvoIsland
//        .builder[Bitstring]()
//        .addObjective(new Objective[Bitstring]("Three", Maximize()) {
//          override protected def objective(sol: Bitstring): Double = 3d
//        })
//        .addModifier(Bitflipper())
//        .addDeletor(DeleteDominated())
//        .build()
//    }
//
//    "require at least one Objective, Mutator, Deletor" in {
//      // Leaving this commented out: it's a compile time error. But I'm leaving it here
//      // to inform people in the future.
//      // val builder = EvvoIsland.builder[Bitstring]().buildLocalEvvo()
//    }
//
//    "allow modification of strategies" in {
//      EvvoIsland
//        .builder[Bitstring]()
//        .addObjective(new Objective[Bitstring]("Three", Maximize()) {
//          override protected def objective(sol: Bitstring): Double = 3d
//        })
//        .addCreator(BitstringGenerator(length = 16))
//        .addModifier(Bitflipper())
//        .addDeletor(DeleteDominated())
//        .withEmigrationStrategy(RandomSampleEmigrationStrategy(3))
//        .withEmigrationTargetStrategy(RoundRobinEmigrationTargetStrategy())
//        .withImmigrationStrategy(AllowAllImmigrationStrategy())
//        .withLoggingStrategy(NullLoggingStrategy())
//        .build()
//    }
  }
}
