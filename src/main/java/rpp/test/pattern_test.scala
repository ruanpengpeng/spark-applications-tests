package rpp.test
import scala.io.Source
object pattern_test {
  def main(args: Array[String]): Unit = {
    val data=Source.fromFile("D://1.txt")
    val pattern ="#.*?#".r
    for( line <- data.getLines()){
      val con = pattern.findAllIn(line)
      con.foreach(println)
    }
    //pattern.findAllIn(x)
  }
}
