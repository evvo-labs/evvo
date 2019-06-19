package com.evvo.examples

package object tsp {

  /**
    * Represents an ordered sequence of visited cities by index.
    */
  type TSPSolution = Vector[Int]


  /**
    * indexed by (from,to) gives distance from first index to second by both objectives
    */
  type DistanceMatrix = IndexedSeq[IndexedSeq[(Double,Double)]]


}
