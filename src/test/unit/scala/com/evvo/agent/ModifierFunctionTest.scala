package com.evvo.agent

import com.evvo.island.population.Scored
import org.scalatest.WordSpec

class ModifierFunctionTest extends WordSpec{
  "ModifierFunction" should {
    "call its function on the input set" in {

      val modifier = new ModifierFunction[Double]("Modifier") {
        override def modify(sols: IndexedSeq[Scored[Double]]): TraversableOnce[Double] = {
          Vector(3)
        }
      }
      assert(modifier.modify(IndexedSeq[Scored[Double]]()) == Vector(3))
    }
  }

}
