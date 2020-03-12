package io.evvo.builtin

import io.evvo.builtin.bitstrings.{Bitflipper, Bitstring}
import org.scalatest.WordSpec

class BitflipperTest extends WordSpec {
  "A Bitflipper" should {
    // Arbitrary for this test, should work with any length
    val bitstringLength = 18
    val allOnes: Bitstring = Bitstring(Vector.fill(bitstringLength)(true))
    val allZeroes: Bitstring = Bitstring(Vector.fill(bitstringLength)(false))

    "flip a random bit" in {
      // With one bit flipped, all ones should have one 0 bit.
      val oneZero = Bitflipper().mutate(allOnes)
      assert(oneZero.bits.count(x => x) == bitstringLength - 1)

      // With one bit flipped, all zeroes should have one 1 bit
      val oneOne = Bitflipper().mutate(allZeroes)
      assert(oneOne.bits.count(x => x) == 1)
    }

    "not care about input size" in {
      assert(Bitflipper().shouldRunWithPartialInput)
    }
  }

}
