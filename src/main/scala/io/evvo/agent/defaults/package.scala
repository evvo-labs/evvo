package io.evvo.agent

package object defaults {
  /** Represents a string of bits, where True is a 1 and False is a 0. This is not the most
    * efficient implementation, but by giving it a name it should be possible to transfer over
    * to some more sophisticated bitstring representation later.
    */
  type Bitstring = Seq[Boolean]
}
