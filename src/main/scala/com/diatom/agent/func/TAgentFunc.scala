package com.diatom.agent.func

/**
  * A function that an agent can apply repeatedly.
  */
trait TAgentFunc {

  /**
    * @return The unique name.
    */
  def name: String = this.toString

}
