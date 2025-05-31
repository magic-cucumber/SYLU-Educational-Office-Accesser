package top.kagg886.eoa.pages.main.home.course.list

import top.kagg886.backend.database.dao.CourseAndRecord


typealias MaybeConflictCourse = List<CourseAndRecord>

val MaybeConflictCourse.hasConflict: Boolean
    get() = this.size > 1

val MaybeConflictCourse.asNoConflict: CourseAndRecord
    get() {
        check(!hasConflict) { "Course has conflict" }
        return this.first()
    }