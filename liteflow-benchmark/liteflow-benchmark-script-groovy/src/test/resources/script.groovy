import cn.hutool.core.collection.ListUtil
import cn.hutool.core.date.DateUtil

import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

def date = DateUtil.parse("2022-10-17 13:31:43")
defaultContext.setData("demoDate", date)

List<String> list = ListUtil.toList("a", "b", "c")

List<String> resultList = list.stream().map(s -> "hello," + s).collect(Collectors.toList())

defaultContext.setData("resultList", resultList)

class Student {
    int studentID
    String studentName
}

Student student = new Student()
student.studentID = 100301
student.studentName = "张三"
defaultContext.setData("student", student)

def a = 3
def b = 2
defaultContext.setData("s1", a * b)