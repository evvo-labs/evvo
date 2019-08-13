package io.evvo.agent

import io.evvo.island.population.Scored
import org.scalatest.WordSpec

class ModifierFunctionTest extends WordSpec {
  "ModifierFunction" should {
    "call its function on the input set" in {

      val modifier = new ModifierFunction[Double]("Modifier") {
        override def modify(sols: IndexedSeq[Scored[Double]]): Iterable[Double] = {
          Vector(3)
        }
      }
      assert(modifier(IndexedSeq[Scored[Double]]()) == Vector(3))
    }
  }

}
