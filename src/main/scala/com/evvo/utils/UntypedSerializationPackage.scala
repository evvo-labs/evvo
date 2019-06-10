package com.evvo.utils

/**
  * A workaround for type erasure messing with serialization. By storing the value as an `Any` and
  * casting to a `T` at runtime, we can manually control typeful deserialization. We also ensure
  * through this class that casting will not cause type errors, since creation of the class can
  * only be done through calling a (strongly typed) method on the companion object.
  *
  * @param storedValue the value to store. Technically type `Any`, but this class can only be
  *                    constructed with a `storedValue` of type `T`.
  * @tparam T the type of the storedValue.
  */
class UntypedSerializationPackage[T] private(private val storedValue: Any) extends Serializable {

  /** @return the value stored in this package cast to the type it had before storage. */
  def content: T = storedValue.asInstanceOf[T]
}

object UntypedSerializationPackage {
  def apply[T](t: T): UntypedSerializationPackage[T] = new UntypedSerializationPackage[T](t)
}
