package com.evvo.examples.professormatching

import com.evvo.examples.professormatching.ProfessorMatching._
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
  * See [[io.evvo.professormatching.ProfPreferences]].
  */
case class ParsedProfPreferences(id: Int,
                                 sectionScheduleToPreference: Map[String, Int],
                                 courseToPreference: Map[Int, Int],
                                 maxSections: Int,
                                 maxPreps: Int) {

  def toProfPreference: ProfPreferences = ProfPreferences(
    id,
    sectionScheduleToPreference.asInstanceOf[Map[ScheduleID, Int]],
    courseToPreference.asInstanceOf[Map[CourseID, Int]],
    maxSections,
    maxPreps)
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
//  println(DataReader.readFromJsonFile("examples/main/scala/com/evvo/professormatching/preferences_mock.json"))
//}
