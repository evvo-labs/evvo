//package io.evvo
//
//import io.evvo.LocalEvvoTestFixtures._
//import io.evvo.agent.{CreatorFunction, MutatorFunction}
//import io.evvo.builtin.deletors.DeleteWorstHalfByRandomObjective
//import io.evvo.island.population.{Minimize(), Objective, ParetoFrontier}
//import io.evvo.island.{EvolutionaryProcess,  StopAfter}
//import io.evvo.tags.{Performance, Slow}
//import org.scalatest.{Matchers, WordSpec}
//
//import scala.concurrent.duration._
//
///** Tests a single island cluster.
//  *
//  * The behavior under test is that an Island can sort a list given the proper modifier and fitness
//  * function, and terminate successfully returning a set of lists.
//  */
//class LocalEvvoTest extends WordSpec with Matchers {
//
//  /** Creates a test Evvo instance running locally, that will use basic swapping
//    * to mutate lists, starting in reverse order, scoring them on the number of inversions.
//    *
//    * @param listLength the length of the lists to sort.
//    * @return
//    */
//  def getEvvo(listLength: Int): EvolutionaryProcess[Solution] = {
//
//    val islandBuilder = EvvoIslandBuilder[Solution]()
//      .addCreator(new ReverseListCreator(listLength))
//      .addModifier(new SwapTwoElementsModifier())
//      .addDeletor(DeleteWorstHalfByRandomObjective())
//      .addObjective(new NumInversions())
//
//    islandBuilder.build()
//  }
//
//  "Local Evvo" should {
//    val timeout = 1
//    val listLength = 6
//    f"be able to sort a list of length $listLength within $timeout seconds" taggedAs (Performance, Slow) in {
//      val terminate = StopAfter(timeout.seconds)
//
//      val evvo = getEvvo(listLength)
//      evvo.runBlocking(terminate)
//      val pareto: ParetoFrontier[Solution] = evvo.currentParetoFrontier()
//      assert(pareto.solutions.exists(_.score(("Inversions", Minimize())) == 0d))
//    }
//  }
//}
//
//object LocalEvvoTestFixtures {
//  type Solution = List[Int]
//
//  class ReverseListCreator(listLength: Int) extends CreatorFunction[Solution]("ReverseCreator") {
//    override def create(): Iterable[Solution] = {
//      Vector((listLength to 1 by -1).toList)
//    }
//  }
//
//  class SwapTwoElementsModifier extends MutatorFunction[Solution]("SwapTwo") {
//    override def mutate(sol: Solution): Solution = {
//      val i = util.Random.nextInt(sol.length)
//      val j = util.Random.nextInt(sol.length)
//      sol.updated(j, sol(i)).updated(i, sol(j))
//    }
//  }
//
//  class NumInversions extends Objective[Solution]("Inversions", Minimize()) {
//    override protected def objective(sol: Solution): Double = {
//      // Old way of doing it: still works, but more clear with tails: left for clarity
//      //      (for ((elem, index) <- sol.zipWithIndex) yield {
//      //        sol.drop(index).count(_ < elem)
//      //      }).sum
//
//      // For each item, add the number of elements in the rest of the list less than this item.
//      // Once you're at the last item, make no change.
//      sol.tails.foldRight(0d)((list, numInversionsSoFar) => {
//        list match {
//          case item :: rest => numInversionsSoFar + rest.count(_ < item)
//          case _ => numInversionsSoFar
//        }
//      })
//    }
//  }
//}
