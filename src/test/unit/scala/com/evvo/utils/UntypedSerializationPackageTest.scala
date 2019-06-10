package com.evvo.utils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.scalatest.{Matchers, WordSpec}

class UntypedSerializationPackageTest extends WordSpec with Matchers {

  "An UntypedSerializationPackage" should {
    "produce correctly typed output for ints" in {
      val three: Int = 3
      val wrappedThree = UntypedSerializationPackage[Int](three)
      val unwrappedThree: Int = wrappedThree.content
      unwrappedThree shouldBe three
    }

    "survive serialization" in {
      val three: Int = 3
      val wrappedThree = UntypedSerializationPackage[Int](three)

      val baos = new ByteArrayOutputStream()
      val outputStream = new ObjectOutputStream(baos)
      outputStream.writeObject(wrappedThree)

      val bais = new ByteArrayInputStream(baos.toByteArray)
      val inputStream = new ObjectInputStream(bais)
      val deserializedObject = inputStream.readObject()
      val deserializedWrappedThree =
        deserializedObject.asInstanceOf[UntypedSerializationPackage[Int]]
      val unwrappedThree: Int = deserializedWrappedThree.content
      unwrappedThree shouldBe three
    }

    "survive with a higher order type" in {
      type OLI = Option[List[Int]]
      val three: OLI = Some(List(3))
      val wrappedThree = UntypedSerializationPackage[OLI](three)

      val baos = new ByteArrayOutputStream()
      val outputStream = new ObjectOutputStream(baos)
      outputStream.writeObject(wrappedThree)

      val bais = new ByteArrayInputStream(baos.toByteArray)
      val inputStream = new ObjectInputStream(bais)
      val deserializedObject = inputStream.readObject()
      val deserializedWrappedThree =
        deserializedObject.asInstanceOf[UntypedSerializationPackage[OLI]]
      val unwrappedThree: OLI = deserializedWrappedThree.content
      unwrappedThree shouldBe three
    }
  }
}
