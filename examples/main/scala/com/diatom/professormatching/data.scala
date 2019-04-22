package com.diatom.professormatching

import com.diatom.professormatching.ProfessorMatching._
import io.circe.generic.auto._
import io.circe.parser._

case class ParsedProblem(professors: Vector[ParsedProfPreferences], sections: Vector[ParsedSection]) {

  def toProblem: Problem = {
    Problem(professors.map(p => p.id -> p.toProfPreference).toMap,
      sections.map(s => s.id -> s.toSection).toMap,
      SectionScheduleMap.scheduleIDtoSchedule)
  }

}

/**
  *
  * @param id                          the professor's id
  * @param sectionScheduleToPreference a mapping of preferences for each schedule
  * @param courseToPreference          a mapping of preferences for each course they want to teach
  * @param numSectionsToPreference     mapping from number of sections to preference
  * @param numPrepsToPreference        a mapping of # unique classes to preference for that #
  */
case class ParsedProfPreferences(id: Int,
                                 sectionScheduleToPreference: Map[String, Int],
                                 courseToPreference: Map[Int, Int],
                                 numSectionsToPreference: Map[Int, Int],
                                 numPrepsToPreference: Map[Int, Int]) {

  def toProfPreference: ProfPreferences = ProfPreferences(
    id,
    sectionScheduleToPreference.asInstanceOf[Map[ScheduleID, Int]],
    courseToPreference.asInstanceOf[Map[CourseID, Int]],
    numSectionsToPreference,
    numPrepsToPreference)
}

case class ParsedSection(id: Int, courseID: Int, scheduleID: String) {
  def toSection: Section = Section(id, courseID, scheduleID)
}

object DataReader {

  def readFromJsonFile(filename: String): Problem = {
    val fileStream = scala.io.Source.fromFile(filename)
    val jsonString = fileStream.getLines().mkString("\n")
    fileStream.close()

    decode[ParsedProblem](jsonString) match {
      case Left(throwable: Throwable) => throw throwable
      case Right(parsedProblem: ParsedProblem) => parsedProblem.toProblem
    }
  }
}

//object Main extends App {
////  For testing
//  println(DataReader.readFromJsonFile("examples/main/scala/com/diatom/professormatching/preferences_mock.json"))
//}
