package io.evvo.island

import io.evvo.island.population.{Maximize, Objective}
import io.evvo.tags.Slow
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import unit.scala.io.evvo.fixtures.testemigrators
import unit.scala.io.evvo.fixtures.testemigrators.{LocalEmigrator, LocalImmigrator}

class EvvoIslandTest extends WordSpec with Matchers with BeforeAndAfter {
  // private because EvvoIsland is private, required to compile.
  private var island1: EvvoIsland[Int] = _
  private var island2: EvvoIsland[Int] = _
  object MaximizeInt extends Objective[Int]("Test", Maximize()) {
    override protected def objective(sol: Int): Double = sol
  }

  before {
    island1 = new EvvoIsland(
      Vector(),
      Vector(),
      Vector(),
      Vector(MaximizeInt),
      new LocalImmigrator[Int](),
      ElitistImmigrationStrategy(),
      new LocalEmigrator[Int](SendToAllEmigrationTargetStrategy()),
      RandomSampleEmigrationStrategy(4),
      LogPopulationLoggingStrategy()
    )

    island2 = new EvvoIsland(
      Vector(),
      Vector(),
      Vector(),
      Vector(MaximizeInt),
      new LocalImmigrator[Int](),
      AllowAllImmigrationStrategy(),
      new LocalEmigrator[Int](SendToAllEmigrationTargetStrategy()),
      RandomSampleEmigrationStrategy(4),
      LogPopulationLoggingStrategy()
    )
  }

  "EvvoIsland" should {
    "use immigration strategy to filter incoming solutions" in {
      island1.addSolutions(Seq(10))
      island2.emigrate()
      island1.immigrate()

      // The three shouldn't be added, because Elitist will prevent anything < 10 from being added
      island1.currentParetoFrontier().solutions should have size 1

      // But 11 should make it through.
      island2.addSolutions(Seq(11))
      island2.emigrate()
      island1.immigrate()
      island1.currentParetoFrontier().solutions.map(_.solution) should be(Set(11))
      testemigrators.reset()
    }

    "emigrate strategies to other islands" taggedAs Slow in {
      // Is island2 is changed by island 1 running, then emigration must have happened.
      island2.currentParetoFrontier().solutions.size shouldBe 0
      island1.addSolutions(Seq(1))
      island1.emigrate()
      island2.immigrate()
      island2.currentParetoFrontier().solutions.size shouldBe 1
      testemigrators.reset()
    }

    "use the emigration strategy to choose which solutions to emigrate" taggedAs Slow in {
      val noEmigrationIsland = new EvvoIsland[Int](
        Vector(),
        Vector(),
        Vector(),
        Vector(MaximizeInt),
        new LocalImmigrator[Int](),
        ElitistImmigrationStrategy(),
        new LocalEmigrator[Int](SendToAllEmigrationTargetStrategy()),
        NoEmigrationEmigrationStrategy,
        LogPopulationLoggingStrategy()
      )

      // Because island2 wasn't changed, in conjunction with the above test, the change in
      // emigration strategy made a difference.
      island2.currentParetoFrontier().solutions.size shouldBe 0
      noEmigrationIsland.emigrate()
      island2.immigrate()
      island2.currentParetoFrontier().solutions.size shouldBe 0
    }
  }

  //  TODO
  //  "Use the provided network topology to register islands with each other" in {
  //    // Expression blocks so we can reuse the names island1, etc.
  //
//      case object NoCreator extends CreatorFunction[Bitstring]("no") {
//        override def create(): Iterable[Bitstring] = Seq()
//      }
//      case object NoModifier extends ModifierFunction[Bitstring]("No") {
//        override def modify(sols: IndexedSeq[Scored[Bitstring]]): Iterable[Bitstring] = Seq()
//      }
//
//      case object OneMax extends Objective[Bitstring]("OneMax", Maximize()) {
//        override protected def objective(sol: Bitstring): Double = sol.count(identity)
//      }
//
//      case object OneMin extends Objective[Bitstring]("OneMin", Minimize()) {
//        override protected def objective(sol: Bitstring): Double = sol.count(identity)
//      }
//
//      // Make sure they aren't creating any solutions
//      val builderOneMax = EvvoIslandBuilder[Bitstring]()
//        .addCreator(NoCreator)
//        .addModifier(NoModifier)
//        .addDeletor(DeleteDominated())
//        .addObjective(OneMax)
//        .withEmigrationStrategy(WholeParetoFrontierEmigrationStrategy())
//        .withEmigrationTargetStrategy(SendToAllEmigrationTargetStrategy())
//
//      val builderOneMaxandMin = builderOneMax.addObjective(OneMin)
//        val _ = {
//          val island1 = builderOneMax.build()
//          val island2 = builderOneMax.build()
//          val island3 = builderOneMax.build()
//
//          // simply constructing the manager registers the islands with each other
//          val mgr = new IslandManager[Bitstring](
//            Seq(island1, island2, island3),
//            FullyConnectedNetworkTopology())
//          island1.immigrate(Seq(Scored(Map(), Seq(true))))
//
//          mgr.runBlocking(StopAfter(1.second))
//          island2.currentParetoFrontier().solutions should have size 1
//          island3.currentParetoFrontier().solutions should have size 1
//        }
//
//        case object NoTopology extends NetworkTopology {
//          override def configure(numIslands: Int): Seq[Connection] = Seq()
//        }
//
//        val _ = {
//          val island1 = builderOneMax.build()
//          val island2 = builderOneMax.build()
//          val mgr =
//            new IslandManager[Bitstring](Seq(island1, island2), NoTopology)
//          island1.immigrate(Seq(Scored(Map(), Seq(true))))
//          mgr.runBlocking(StopAfter(1.second))
//
//          island2.currentParetoFrontier().solutions should have size 0
//        }
//      }
//  }
}
