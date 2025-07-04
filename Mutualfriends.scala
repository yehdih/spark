import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object MutualFriends {
  def main(args: Array[String]): Unit = {

    // Initialize Spark Context
    val conf = new SparkConf().setAppName("Mutual Friends").setMaster("local[*]")
    val sc = new SparkContext(conf)

    //-------------------------------
    // Define the pairs function
    //-------------------------------
    // This function generates ((userA, userB), friendList) key-value pairs
    def pairs(str: Array[String]) = {
      val users = str(1).split(",")
      val user = str(0)
      val n = users.length
      for (i <- 0 until n) yield {
        val pair =
          if (user < users(i)) {
            (user, users(i))
          } else {
            (users(i), user)
          }
        (pair, users)
      }
    }

    //-------------------------------
    // Main Spark Processing
    //-------------------------------

    // Step 1: Load the input file
    val data = sc.textFile("soc-LiveJournal1Adj.txt")

    // Step 2: Split each line by tab, keep valid rows
    val data1 = data
      .map(x => x.split("\t"))
      .filter(li => (li.size == 2))

    // Step 3: Generate user pairs and reduce by intersecting their friend lists
    val pairCounts = data1
      .flatMap(pairs) // ((userA,userB), friendListA)
      .reduceByKey((list1, list2) => list1.intersect(list2))

    // Step 4: Format as tab-separated output
    val p1 = pairCounts
      .map {
        case ((userA, userB), mutualFriends) =>
          s"$userA\t$userB\t${mutualFriends.mkString(",")}"
      }

    // Step 5: Save complete output
    p1.saveAsTextFile("output")

    //-------------------------------
    // Extract mutual friends for specific pairs
    //-------------------------------

    var ans = ""

    // Example pair: 0 and 4
    val p2 = p1
      .map(_.split("\t"))
      .filter(x => x.size == 3)
      .filter(x => x(0) == "0" && x(1) == "4")
      .flatMap(x => x(2).split(","))
      .collect()

    ans = ans + "0" + "\t" + "4" + "\t" + p2.mkString(",") + "\n"

    // Example pair: 20 and 22939
    val p3 = p1
      .map(_.split("\t"))
      .filter(x => x.size == 3)
      .filter(x => x(0) == "20" && x(1) == "22939")
      .flatMap(x => x(2).split(","))
      .collect()

    ans = ans + "20" + "\t" + "22939" + "\t" + p3.mkString(",") + "\n"

    // Example pair: 1 and 29826
    val p4 = p1
      .map(_.split("\t"))
      .filter(x => x.size == 3)
      .filter(x => x(0) == "1" && x(1) == "29826")
      .flatMap(x => x(2).split(","))
      .collect()

    ans = ans + "1" + "\t" + "29826" + "\t" + p4.mkString(",") + "\n"

    // Example pair: 19272 and 6222
    val p5 = p1
      .map(_.split("\t"))
      .filter(x => x.size == 3)
      .filter(x => x(0) == "19272" && x(1) == "6222")
      .flatMap(x => x(2).split(","))
      .collect()

    ans = ans + "6222" + "\t" + "19272" + "\t" + p5.mkString(",") + "\n"

    // Example pair: 28041 and 28056
    val p6 = p1
      .map(_.split("\t"))
      .filter(x => x.size == 3)
      .filter(x => x(0) == "28041" && x(1) == "28056")
      .flatMap(x => x(2).split(","))
      .collect()

    ans = ans + "28041" + "\t" + "28056" + "\t" + p6.mkString(",") + "\n"

    // Save the specific pairs’ results
    val answer = sc.parallelize(Seq(ans))
    answer.saveAsTextFile("output1")

    // Stop the Spark context
    sc.stop()
  }
}
