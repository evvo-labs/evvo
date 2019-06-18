package com.evvo.agent

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.evvo.agent.defaults.DeleteWorstHalfByRandomObjective
import com.evvo.island.population.Scored
import org.scalatest.{Matchers, WordSpec}

import scala.collection.TraversableOnce

/**
  * Creator, Mutator, and Deletor Functions all have to be serializable for akka actor
  * deployment to work.
  */
class FunctionSerializabilityTest extends WordSpec with Matchers {

  def serializationRoundtrip[T <: NamedFunction](t: T): Unit = {
    // write the object to an output stream
    val baos = new ByteArrayOutputStream()
    val outputStream = new ObjectOutputStream(baos)
    outputStream.writeObject(t)

    // read the object back in, as a T
    val bais = new ByteArrayInputStream(baos.toByteArray)
    val inputStream = new ObjectInputStream(bais)
    val deserializedObject = inputStream.readObject()

    // ensure casting back to a T works
    val deserializedT: T = deserializedObject.asInstanceOf[T]

    // Can't compare classes directly, so just make sure we got something relatively similar
    t.name shouldBe deserializedT.name
  }

  "Creator, Mutator, and Deletor Functions" should {
    "be serializable when extended as anonymous classes" in {
      val c = new CreatorFunction[Double]("c") {
        override def create(): TraversableOnce[Double] = Vector(3)
      }

      val m = new ModifierFunction[Double]("m") {
        override def modify(sols: IndexedSeq[Scored[Double]]): TraversableOnce[Double] = {
          Vector(sols.head.solution)
        }
      }

      val d = new DeletorFunction[Double]("d") {
        override def delete(sols: IndexedSeq[Scored[Double]]): TraversableOnce[Scored[Double]] = {
          sols.take(sols.length / 2)
        }
      }

      serializationRoundtrip(c)
      serializationRoundtrip(m)
      serializationRoundtrip(d)
    }

    "be serializable when operating on base types" in {
      class C extends CreatorFunction[Double]("c") {
        override def create(): TraversableOnce[Double] = Vector(3)
      }

      class M extends ModifierFunction[Double]("d") {
        override def modify(sols: IndexedSeq[Scored[Double]]): TraversableOnce[Double] = {
          Vector(sols.head.solution)
        }
      }

      class D extends DeletorFunction[Double]("m") {
        override def delete(sols: IndexedSeq[Scored[Double]]): TraversableOnce[Scored[Double]] = {
          sols.take(sols.length / 2)
        }
      }

      serializationRoundtrip(new C())
      serializationRoundtrip(new M())
      serializationRoundtrip(new D())
    }

    "be serializable when operating on generic types" in {

      type G = Set[Option[Double]]
      class C extends CreatorFunction[G]("c") {
        override def create(): TraversableOnce[G] = Vector(Set(Some(3)), Set(None, Some(5d)))
      }

      class M extends MutatorFunction[G]("d") {
        override def mutate(sol: G): G = {
          sol
        }
      }

      class D extends DeletorFunction[G]("m") {
        override def delete(sols: IndexedSeq[Scored[G]]): TraversableOnce[Scored[G]] = {
          sols.take(sols.length / 2)
        }
      }

      serializationRoundtrip(new C())
      serializationRoundtrip(new M())
      serializationRoundtrip(new D())
    }

    "be serializable when operating on case classes" in {
      // CC is a case class defined at the bottom of this file.
      // If it is defined within this test, that class is anonymous and cannot be
      // deserialized. This is not an issue for end users, as they should be defining their classes
      // in

      class C extends CreatorFunction[CC]("c") {
        override def create(): TraversableOnce[CC] = Vector(CC(List(3)))
      }

      class M extends MutatorFunction[CC]("d") {
        override def mutate(sol: CC): CC = {
          sol
        }
      }

      class D extends DeletorFunction[CC]("m") {
        override def delete(sols: IndexedSeq[Scored[CC]]): TraversableOnce[Scored[CC]] = {
          sols.take(sols.length / 2)
        }
      }


      serializationRoundtrip(new C())
      serializationRoundtrip(new M())
      serializationRoundtrip(new D())
    }

    "be serializable when operating on on classes" in {
      // Clazz is defined at the bottom of this file. See case class test for reasoning.

      class C extends CreatorFunction[Clazz]("c") with Serializable {
        override def create(): TraversableOnce[Clazz] = Vector(new Clazz(5))
      }

      class M extends MutatorFunction[Clazz]("d") {
        override def mutate(sol: Clazz): Clazz = {
          sol
        }
      }

      class D extends DeletorFunction[Clazz]("m") {
        override def delete(sols: IndexedSeq[Scored[Clazz]]): TraversableOnce[Scored[Clazz]] = {
          sols.take(sols.length / 2)
        }
      }

      serializationRoundtrip(new C())
      serializationRoundtrip(new M())
      serializationRoundtrip(new D())
    }
  }

}

class Clazz(val x: Int) extends Serializable

case class CC(x: List[Int])
