package com.diatom

import java.time.{DayOfWeek, Instant}
import java.time.LocalTime.parse
import java.time.LocalTime

import scala.concurrent.duration._
import com.diatom.ProfessorMatching.SectionSchedule.SectionSchedule
import com.diatom.island.{SingleIslandEvvo, TerminationCriteria}

/**
  * Matches professors with courses, assuming:
  * - Each course has been already assigned a timeslot
  * - There are more than twice as many sections as professors
  */
object ProfessorMatching {

  type ProfID >: Int
  type SectionID >: Int
  type CourseID >: Int
  type Sol = Map[ProfID, Set[SectionID]]

  //  implicit def int2profid(i: Int) = i.asInstanceOf[ProfID]

  /**
    *
    * @param id                          the professor's id
    * @param sectionScheduleToPreference a mapping of preferences for each schedule
    * @param courseToPreference          a mappping of preferences for each course they want to teach
    * @param numSectionsToPreference     mapping from number of sections to preference
    * @param numPrepsToPreference           a mapping of # unique classes to preference for that #
    */
  case class ProfPreferences(id: ProfID,
                             sectionScheduleToPreference: Map[SectionSchedule, Int],
                             courseToPreference: Map[CourseID, Int],
                             numSectionsToPreference: Map[Int, Int],
                             numPrepsToPreference: Map[Int, Int])

  object SectionSchedule extends Enumeration {

    case class SectionSchedule(timeSlots: TimeSlot*) extends super.Val {

      def overlaps(that: SectionSchedule): Boolean = {
        this.timeSlots.exists(t =>
          that.timeSlots.exists(_.overlaps(t)))
      }
    }

    case class TimeSlot(dayOfWeek: DayOfWeek,
                        startTime: LocalTime,
                        endTime: LocalTime) {

      def overlaps(that: TimeSlot): Boolean = {
        (this.dayOfWeek == that.dayOfWeek
          && ((this.startTime.isBefore(that.startTime) && that.startTime.isBefore(this.endTime))
          || (that.startTime.isBefore(this.startTime) && this.startTime.isBefore(that.endTime))))

      }
    }


    // TODO: Add in all the timeslots for all schedules
    //       (https://registrar.northeastern.edu/app/uploads/semcrsseq-flsp-new.pdf)
    val SCHED1 = SectionSchedule(
      TimeSlot(DayOfWeek.MONDAY, parse("08:00"), parse("09:05")),
      TimeSlot(DayOfWeek.WEDNESDAY, parse("08:00"), parse("09:05")),
      TimeSlot(DayOfWeek.THURSDAY, parse("08:00"), parse("09:05")))

    val SCHED2 = SectionSchedule(
      TimeSlot(DayOfWeek.MONDAY, parse("09:15"), parse("10:20")),
      TimeSlot(DayOfWeek.WEDNESDAY, parse("09:15"), parse("10:20")),
      TimeSlot(DayOfWeek.THURSDAY, parse("09:15"), parse("10:20")))

    val SCHEDP = SectionSchedule(
      TimeSlot(DayOfWeek.MONDAY, parse("08:00"), parse("10:20")),
      TimeSlot(DayOfWeek.WEDNESDAY, parse("08:00"), parse("10:20")),
      TimeSlot(DayOfWeek.THURSDAY, parse("08:00"), parse("10:20")))
  }

  import SectionSchedule._


  case class Section(id: SectionID, course: CourseID, schedule: SectionSchedule)


  def readProf(): Map[ProfID, ProfPreferences] = {
    val schedulePref1 = Map(SCHED1 -> 5, SCHED2 -> 0, SCHEDP -> 3)
    val coursePref1: Map[CourseID, Int] = Map(1 -> 5, 2 -> 0)
    val numCoursePref1 = Map(0 -> 0, 1 -> 1, 2 -> 4, 3 -> 5, 4 -> 2)
    val prepsPref1 = Map(0 -> 0, 1 -> 3, 2 -> 5, 3 -> 4, 4 -> 2)
    val prof1 = ProfPreferences(1, schedulePref1, coursePref1, numCoursePref1, prepsPref1)

    val schedulePref2 = Map(SCHED1 -> 0, SCHED2 -> 5, SCHEDP -> 3)
    val coursePref2: Map[CourseID, Int] = Map(1 -> 0, 2 -> 5)
    val numCoursePref2 = Map(0 -> 0, 1 -> 1, 2 -> 4, 3 -> 5, 4 -> 2)
    val prepsPref2 = Map(0 -> 0, 1 -> 3, 2 -> 5, 3 -> 4, 4 -> 2)
    val prof2 = ProfPreferences(2, schedulePref2, coursePref2, numCoursePref2, prepsPref2)

    Map(
      1 -> prof1,
      2 -> prof2,
    )
  }

  val idToProf: Map[ProfID, ProfPreferences] = readProf()

  val idToSection = Map[SectionID, Section](
    1 -> Section(1, 1, SCHED1),
    2 -> Section(2, 1, SCHED1),
    3 -> Section(3, 1, SCHED2),
    4 -> Section(4, 2, SCHED2),
    5 -> Section(5, 2, SCHEDP),
    6 -> Section(6, 2, SCHEDP),
  )


  // =================================== FITNESS ===================================================
  val sumProfessorSchedulePreferences: FitnessFunctionType[Sol] = sol => {
    -sol.foldLeft(0) {
      case (soFar, (profID, sections)) =>
        soFar + sections.foldLeft(0)((tot, sectionID) =>
          tot + idToProf(profID).sectionScheduleToPreference(idToSection(sectionID).schedule))
    }
  }

  val sumProfessorCoursePreferences: FitnessFunctionType[Sol] = sol => {
    -sol.foldLeft(0) {
      case (soFar, (profID, sections)) =>
        soFar + sections.foldLeft(0)((tot, sectionID) =>
          tot + idToProf(profID).courseToPreference(idToSection(sectionID).course))
    }
  }

  val sumProfessorSectionCountPreferences: FitnessFunctionType[Sol] = sol => {
    -sol.foldLeft(0) {
      case (soFar, (profID, sections)) =>
        soFar + idToProf(profID).numSectionsToPreference(sections.size)
    }
  }

  val sumProfessorNumPrepsPreferences: FitnessFunctionType[Sol] = sol => {
    -sol.foldLeft(0) {
      case (soFar, (profID, sections)) =>
        soFar + idToProf(profID).numPrepsToPreference(sections.map(idToSection(_).course).size)
    }
  }

  val validScheduleCreator: CreatorFunctionType[Sol] = () => {
    Vector.fill(10)(idToProf.keysIterator.zipAll(
      util.Random.shuffle(idToSection.keys)
        .grouped(idToSection.size / idToProf.size + 1)
        .map(_.toSet),
      -1,
      Set[SectionID]()).toMap).toSet
  }

  def randomKey[A, B](map: Map[A, B]): A = {
    map.keysIterator.drop(util.Random.nextInt(map.size)).next()
  }

  def randomElement[A](s: Set[A]): A = {
    s.toVector(util.Random.nextInt(s.size))
  }

  val swapTwoCourses: MutatorFunctionType[Sol] = (sols: Set[TScored[Sol]]) => {
    def swap(sol: Sol): Sol = {
      val prof1: ProfPreferences = idToProf(randomKey(idToProf))
      val prof2: ProfPreferences = {
        var prof2maybe:ProfPreferences = null
        do {
          prof2maybe = idToProf(randomKey(idToProf))
        } while (prof2maybe == prof1)
        prof2maybe
      }

      val courses1 = sol(prof1.id)
      val courses2 = sol(prof2.id)

      val course1 = randomElement(courses1)
      val course2 = randomElement(courses2)

      sol.updated(prof1.id, courses1 - course1 + course2)
        .updated(prof2.id, courses2 - course2 + course1)
    }

    sols.map(_.solution).map(swap)
  }
  val balanceCourseload: MutatorFunctionType[Sol] = (sols: Set[TScored[Sol]]) => {
    def swap(sol: Sol): Sol = {
      val prof1: ProfPreferences = idToProf(randomKey(idToProf))
      val prof2: ProfPreferences = {
        var prof2maybe: ProfPreferences = null
        do {
          prof2maybe = idToProf(randomKey(idToProf))
        } while (prof2maybe == prof1)
        prof2maybe
      }


      val courses1 = sol(prof1.id)
      val courses2 = sol(prof2.id)

      if (courses1.size == courses2.size) {
        return sol
      }

      val (profWithMore, coursesMore, profWithLess, coursesLess) = if (courses1.size < courses2.size) {
        (prof2, courses2, prof1, courses1)
      } else {
        (prof1, courses1, prof2, courses2)
      }

      val courseToTransfer = randomElement(coursesMore)

      sol.updated(profWithMore.id, coursesMore - courseToTransfer)
        .updated(profWithLess.id, coursesLess + courseToTransfer)
    }

    sols.map(_.solution).map(swap)
  }



  val deleteWorstHalf: DeletorFunctionType[Sol] = (s: Set[TScored[Sol]]) => {
    if (s.isEmpty) {
      s
    } else {
      val funcs = s.head.score.keys.toVector
      val func = funcs(util.Random.nextInt(funcs.size))

      s.toVector.sortBy(_.score(func)).take(s.size / 2).toSet
    }
  }

  def main(args: Array[String]): Unit = {

    val island = SingleIslandEvvo.builder()
      .addFitness(sumProfessorSchedulePreferences, "Sched")
      .addFitness(sumProfessorCoursePreferences, "Course")
      .addFitness(sumProfessorNumPrepsPreferences, "#Prep")
      .addFitness(sumProfessorSectionCountPreferences, "#Section")
      .addCreator(validScheduleCreator)
      .addMutator(swapTwoCourses)
      .addMutator(balanceCourseload)
      .addDeletor(deleteWorstHalf)
      .build()

    val pareto = island.run(TerminationCriteria(1.second))
    println(pareto)
  }
}
