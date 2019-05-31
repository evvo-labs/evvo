package com.evvo.professormatching

import java.io.File
import java.time.LocalTime.parse
import java.time.{DayOfWeek, LocalTime}

import akka.actor.ActorSystem
import com.evvo._
import com.evvo.agent._
import com.evvo.island.population.{Maximize, Objective}
import com.evvo.island.{EvvoIsland, EvvoIslandActor, IslandManager, TerminationCriteria}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
  * Matches professors with courses, assuming:
  * - Each course has been already assigned a timeslot
  * - There are more than twice as many sections as professors
  */
object ProfessorMatching {
  // these are superclasses of base types so that you can use the base types to create them,
  // but they are separate types so they can't be used interchangeably
  type ProfID >: Int
  type SectionID >: Int
  type CourseID >: Int
  type ScheduleID >: String
  type PMSolution = Map[ProfID, Set[SectionID]] // short for professor-matching solution

  case class Problem(profIDtoPref: Map[ProfID, ProfPreferences],
                     sectionIDtoSection: Map[SectionID, Section],
                     scheduleIDtoSchedule: Map[ScheduleID, SectionSchedule])

  /**
    *
    * @param id                          the professor's id
    * @param sectionScheduleToPreference a mapping of preferences for each schedule
    * @param courseToPreference          a mapping of preferences for each course they want to teach
    * @param maxSections                 maximum number of sections
    * @param maxPreps                    maximum number of preps
    */
  case class ProfPreferences(id: ProfID,
                             sectionScheduleToPreference: Map[ScheduleID, Int],
                             courseToPreference: Map[CourseID, Int],
                             maxSections: Int,
                             maxPreps: Int)

  case class Section(id: SectionID, courseID: CourseID, scheduleID: ScheduleID)

  case class SectionSchedule(id: ScheduleID, timeSlots: TimeSlot*) {
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

  object SectionScheduleMap {
    // TODO: Add in all the timeslots for all schedules
    //       (https://registrar.northeastern.edu/app/uploads/semcrsseq-flsp-new.pdf)
    private val SCHED1 = SectionSchedule("1",
      TimeSlot(DayOfWeek.MONDAY, parse("08:00"), parse("09:05")),
      TimeSlot(DayOfWeek.WEDNESDAY, parse("08:00"), parse("09:05")),
      TimeSlot(DayOfWeek.THURSDAY, parse("08:00"), parse("09:05")))

    private val SCHED2 = SectionSchedule("2",
      TimeSlot(DayOfWeek.MONDAY, parse("09:15"), parse("10:20")),
      TimeSlot(DayOfWeek.WEDNESDAY, parse("09:15"), parse("10:20")),
      TimeSlot(DayOfWeek.THURSDAY, parse("09:15"), parse("10:20")))

    private val SCHEDP = SectionSchedule("P",
      TimeSlot(DayOfWeek.MONDAY, parse("08:00"), parse("10:20")),
      TimeSlot(DayOfWeek.WEDNESDAY, parse("08:00"), parse("10:20")),
      TimeSlot(DayOfWeek.THURSDAY, parse("08:00"), parse("10:20")))

    val scheduleIDtoSchedule: Map[ScheduleID, SectionSchedule] = Map(
      "1" -> SCHED1,
      "2" -> SCHED2,
      "P" -> SCHEDP
    )
  }

  // =================================== MAIN ===================================================
  def main(args: Array[String]): Unit = {

    // TODO rename fitness to objective function
    //      and provide class to create objective functions
    val islandBuilder = EvvoIsland.builder()
      .addObjective(Objective(sumProfessorSchedulePreferences, "Sched", Maximize))
      .addObjective(Objective(sumProfessorCoursePreferences, "Course", Maximize))
      .addObjective(Objective(sumProfessorNumPrepsPreferences, "#Prep", Maximize))
      .addObjective(Objective(sumProfessorSectionCountPreferences, "#Section", Maximize))
      .addCreator(CreatorFunction(validScheduleCreator, "creator"))
      .addMutator(MutatorFunction(swapTwoCourses, "swapTwoCourses"))
      .addMutator(MutatorFunction(balanceCourseload, "balanceCourseload"))

    val config = ConfigFactory
      .parseFile(new File("src/main/resources/application.conf"))
      .resolve()

    implicit val system: ActorSystem = ActorSystem("EvvoNode", config)

    val numIslands = 5
    val manager = IslandManager.from[PMSolution](numIslands, islandBuilder,
      userConfig = "src/main/resources/remoting_example.conf")
    manager.runBlocking(TerminationCriteria(1.second))
    val pareto = manager.currentParetoFrontier()
    manager.poisonPill()
    println(f"Pareto Frontier:\n${pareto}")
    system.terminate()
  }

  def readProblem(): Problem = {
    DataReader.readFromJsonFile(
      "examples/main/scala/com/evvo/professormatching/preferences_mock.json")
  }

  private val problem: Problem = readProblem()
  private val idToProf: Map[ProfID, ProfPreferences] = problem.profIDtoPref
  private val idToSection = problem.sectionIDtoSection
  private val idToSchedule = problem.scheduleIDtoSchedule


  // =================================== FITNESS ===================================================
  val sumProfessorSchedulePreferences: ObjectiveFunctionType[PMSolution] = sol => {
    sol.foldLeft(0) {
      case (soFar, (profID, sections)) =>
        val prof = idToProf(profID)
        soFar + sections.foldLeft(0)((tot, sectionID) => {
          val section: Section = idToSection(sectionID)
          val schedule: SectionSchedule = idToSchedule(section.scheduleID)
          tot + prof.sectionScheduleToPreference(schedule.id)
        })
    }
  }

  val sumProfessorCoursePreferences: ObjectiveFunctionType[PMSolution] = sol => {
    sol.foldLeft(0) {
      case (soFar, (profID, sections)) =>
        soFar + sections.foldLeft(0)((tot, sectionID) => {
          tot + idToProf(profID)
            .courseToPreference(
              idToSection(sectionID).courseID)
        })
    }
  }

  val sumProfessorSectionCountPreferences: ObjectiveFunctionType[PMSolution] = sol => {
    sol.foldLeft(0) {
      case (soFar, (profID, sections)) =>
        soFar + (if (idToProf(profID).maxSections < sections.size) 1 else 0)
    }
  }

  val sumProfessorNumPrepsPreferences: ObjectiveFunctionType[PMSolution] = sol => {
    sol.foldLeft(0) {
      case (soFar, (profID, sections)) =>
        soFar + (
          if (idToProf(profID).maxPreps < sections.map(idToSection(_).courseID).size) {
            1
          } else {
            0
          })
    }
  }

  // =================================== CREATOR ==================================================
  val validScheduleCreator: CreatorFunctionType[PMSolution] = () => {
    Vector.fill(10)(idToProf.keysIterator.zip( // scalastyle:ignore magic.number
      util.Random.shuffle(idToSection.keys.toVector)
        .grouped(idToSection.size / idToProf.size + 1)
        .map(_.toSet)).toMap).toSet
  }

  def randomKey[A, B](map: Map[A, B]): A = {
    map.keysIterator.drop(util.Random.nextInt(map.size)).next()
  }

  def randomElement[A](s: Set[A]): A = {
    s.toVector(util.Random.nextInt(s.size))
  }

  // =================================== MUTATOR ===================================================
  val swapTwoCourses: MutatorFunctionType[PMSolution] = {
    sols => {
      val lidToProf = idToProf

      def swap(sol: PMSolution): PMSolution = {
        val prof1: ProfPreferences = lidToProf(randomKey(lidToProf))
        val prof2: ProfPreferences = {
          var prof2maybe: Option[ProfPreferences] = None
          do {
            prof2maybe = Some(lidToProf(randomKey(lidToProf)))
          } while (prof2maybe.contains(prof1))
          prof2maybe.get
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
  }

  val balanceCourseload: MutatorFunctionType[PMSolution] = {
    val lidToProf = idToProf
    sols => {
      def swap(sol: PMSolution): PMSolution = {
        val prof1: ProfPreferences = lidToProf(randomKey(lidToProf))
        val prof2: ProfPreferences = {
          var prof2maybe: Option[ProfPreferences] = None
          do {
            prof2maybe = Some(lidToProf(randomKey(lidToProf)))
          } while (prof2maybe.contains(prof1))
          prof2maybe.get
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
  }


  // =================================== DELETOR ===================================================
  val deleteWorstHalf: DeletorFunctionType[PMSolution] = s => {
    if (s.isEmpty) {
      s
    } else {
      val funcs = s.head.score.keys.toVector
      val func = funcs(util.Random.nextInt(funcs.size))

      s.toVector.sortBy(_.score(func)).take(s.size / 2).toSet
    }
  }
}
