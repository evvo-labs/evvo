package com.evvo.examples.tsp

import org.scalatest.WordSpec

class TSP2Test extends WordSpec {

  "TSP2" should {
    "be able to parse Euclidean points" in {
      val coords1 = Vector((0.0, 0.0), (3.0, 4.0), (4.0, 4.0))
      val coords2 = Vector((6.0, 8.0), (0.0, 0.0), (6.0, 6.0))
      val tsp = TSP2.fromEuclideanPoints(coords1, coords2)
      assert(tsp.distance(0)(0) == (0.0, 0.0))
      assert(tsp.distance(0)(1) == (5.0, 10.0))
      assert(tsp.distance(1)(0) == (5.0, 10.0))
      assert(tsp.distance(1)(2) == (1.0, math.sqrt(72.0)))
      assert(tsp.distance(2)(1) == (1.0, math.sqrt(72.0)))
    }
  }
}
