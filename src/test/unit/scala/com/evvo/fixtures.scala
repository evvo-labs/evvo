package com.evvo

import akka.event.LoggingAdapter

object NullLogger extends LoggingAdapter {
  override def isErrorEnabled: Boolean = false

  override def isWarningEnabled: Boolean = false

  override def isInfoEnabled: Boolean = false

  override def isDebugEnabled: Boolean = false

  override protected def notifyError(message: String): Unit = ()

  override protected def notifyError(cause: Throwable, message: String): Unit = ()

  override protected def notifyWarning(message: String): Unit = ()

  override protected def notifyInfo(message: String): Unit = ()

  override protected def notifyDebug(message: String): Unit = ()
}
