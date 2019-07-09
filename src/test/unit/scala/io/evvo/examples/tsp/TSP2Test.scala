package unit.scala.io.evvo.examples.tsp

import io.evvo.examples.tsp.TSP2
import org.scalatest.{Matchers, WordSpec}

class TSP2Test extends WordSpec with Matchers {

  "TSP2" should {
    val coords1 = Vector((0.0, 0.0), (3.0, 4.0), (4.0, 4.0))
    val coords2 = Vector((6.0, 8.0), (0.0, 0.0), (6.0, 6.0))
    val tsp = TSP2.fromEuclideanPoints(coords1, coords2)
    "be able to parse Euclidean points" in {
      tsp.cost1(0, 0) shouldBe 0.0
      tsp.cost2(0, 0) shouldBe 0.0
      tsp.cost1(0, 1) shouldBe 5.0
      tsp.cost2(0, 1) shouldBe 10.0
      tsp.cost1(1, 0) shouldBe 5.0
      tsp.cost2(1, 0) shouldBe 10.0
      tsp.cost1(1, 2) shouldBe 1.0
      tsp.cost2(1, 2) shouldBe math.sqrt(72.0)
      tsp.cost1(2, 1) shouldBe 1.0
      tsp.cost2(2, 1) shouldBe math.sqrt(72.0)
    }

    "be able to generate the sorted list of cities by distance from a city" in {
      val closestTo1 = tsp.cost1.closestNCities(origin = 1, n = 2)
      closestTo1 shouldBe IndexedSeq(2, 0)
    }
  }
}
