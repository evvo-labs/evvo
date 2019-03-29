package com.diatom.agent.func

/**
  * A function that an agent can apply repeatedly.
  */
trait TAgentFunc {
  // TODO need to go through and actually mandate names being passed, toString doesn't cut it
  /**
    * @return The unique name.
    */
  def name: String = this.toString

}
