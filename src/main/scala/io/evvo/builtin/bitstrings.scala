package io.evvo.builtin

import io.evvo.agent.{CreatorFunction, MutatorFunction}

/** Holds the definition of Bitstrings and operators on Bitstrings. */
object bitstrings {

  /** Represents a string of bits, where True is a 1 and False is a 0. This is not the most
    * efficient implementation, but by giving it a name it should be possible to transfer over
    * to some more sophisticated bitstring representation later.
    */
  type Bitstring = Seq[Boolean]

  /** A creator that generates `Bitstring`s by filling them with random bits.
    *
    * @param length         how long each `Bitstring` should be
    * @param proportionOnes the proportion of bits that start as 1
    */
  case class BitstringGenerator(length: Int, proportionOnes: Double = 0.5)
      extends CreatorFunction[Bitstring]("BitstringGenerator") {
    override def create(): Iterable[Bitstring] = {
      // `<`, because we want the proportion of `true` to increase if `proportionOnes` increases
      Vector.fill(32)(Vector.fill(length)(util.Random.nextDouble() < proportionOnes))
    }
  }

  /** A mutator for `Bitstring`s that swaps two random bits in a solution. */
  case class Bitswapper() extends MutatorFunction[Bitstring]("Bitswapper") {
    override def mutate(bitstring: Bitstring): Bitstring = {
      val index1 = util.Random.nextInt(bitstring.length)
      val index2 = util.Random.nextInt(bitstring.length)

      // not mutation, doesn't need an intermediate temp variable
      bitstring.updated(index1, bitstring(index2)).updated(index2, bitstring(index1))
    }
  }

  /** A mutator for `Bitstring`s that flips a random bit in the bitstring.
    *
    * @param numInputs The number of solutions to request in the contents of each
    *                  input set
    */
  case class Bitflipper(override val numInputs: Int = 32)
      extends MutatorFunction[Bitstring]("Bitflipper") {
    override def mutate(bitstring: Bitstring): Bitstring = {
      val index = util.Random.nextInt(bitstring.length)
      bitstring.updated(index, !bitstring(index))
    }
  }
}
