package com.hrshd1eux.attendly.data.repository

import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.data.local.AttendlyDatabase
import com.hrshd1eux.attendly.data.local.dao.ClassSessionDao
import com.hrshd1eux.attendly.data.local.dao.SubjectDao
import com.hrshd1eux.attendly.data.local.dao.TimetableSessionDao
import com.hrshd1eux.attendly.data.local.entity.ClassSessionEntity
import com.hrshd1eux.attendly.data.local.entity.SubjectEntity
import com.hrshd1eux.attendly.data.local.entity.TimetableSessionEntity
import com.hrshd1eux.attendly.domain.repository.BackupRestoreRepository
import com.hrshd1eux.attendly.domain.repository.BackupValidationResult
import androidx.room.withTransaction
import android.content.Context
import com.hrshd1eux.attendly.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRestoreRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AttendlyDatabase,
    private val subjectDao: SubjectDao,
    private val timetableDao: TimetableSessionDao,
    private val classSessionDao: ClassSessionDao
) : BackupRestoreRepository {

    override suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("appVersion", "1.0.0")

        val subjectsJson = JSONArray()
        // Synchronously read all subjects
        val subjects = subjectDao.getSubjectByName("") // or custom query
        // Read directly from database
        val allSubjects = database.openHelper.readableDatabase.query("SELECT * FROM subjects").use { cursor ->
            val list = mutableListOf<SubjectEntity>()
            while (cursor.moveToNext()) {
                list.add(
                    SubjectEntity(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        shortCode = cursor.getString(cursor.getColumnIndexOrThrow("shortCode")),
                        colorKey = cursor.getString(cursor.getColumnIndexOrThrow("colorKey")),
                        targetPercentage = cursor.getInt(cursor.getColumnIndexOrThrow("targetPercentage")),
                        location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
                        semesterStartDate = if (cursor.isNull(cursor.getColumnIndexOrThrow("semesterStartDate"))) null else cursor.getString(cursor.getColumnIndexOrThrow("semesterStartDate")),
                        semesterEndDate = if (cursor.isNull(cursor.getColumnIndexOrThrow("semesterEndDate"))) null else cursor.getString(cursor.getColumnIndexOrThrow("semesterEndDate")),
                        initialPresent = cursor.getInt(cursor.getColumnIndexOrThrow("initialPresent")),
                        initialAbsent = cursor.getInt(cursor.getColumnIndexOrThrow("initialAbsent")),
                        isArchived = cursor.getInt(cursor.getColumnIndexOrThrow("isArchived")) == 1,
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
                    )
                )
            }
            list
        }

        for (s in allSubjects) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("shortCode", s.shortCode)
            obj.put("colorKey", s.colorKey)
            obj.put("targetPercentage", s.targetPercentage)
            obj.put("location", s.location)
            obj.put("semesterStartDate", s.semesterStartDate ?: JSONObject.NULL)
            obj.put("semesterEndDate", s.semesterEndDate ?: JSONObject.NULL)
            obj.put("initialPresent", s.initialPresent)
            obj.put("initialAbsent", s.initialAbsent)
            obj.put("isArchived", s.isArchived)
            obj.put("createdAt", s.createdAt)
            obj.put("updatedAt", s.updatedAt)
            subjectsJson.put(obj)
        }
        root.put("subjects", subjectsJson)

        val ttJson = JSONArray()
        val allTimetable = database.openHelper.readableDatabase.query("SELECT * FROM timetable_sessions").use { cursor ->
            val list = mutableListOf<TimetableSessionEntity>()
            while (cursor.moveToNext()) {
                list.add(
                    TimetableSessionEntity(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        subjectId = cursor.getLong(cursor.getColumnIndexOrThrow("subjectId")),
                        dayOfWeek = cursor.getInt(cursor.getColumnIndexOrThrow("dayOfWeek")),
                        startMinute = cursor.getInt(cursor.getColumnIndexOrThrow("startMinute")),
                        endMinute = cursor.getInt(cursor.getColumnIndexOrThrow("endMinute")),
                        location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
                        attendanceUnitPolicy = cursor.getString(cursor.getColumnIndexOrThrow("attendanceUnitPolicy")),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
                    )
                )
            }
            list
        }

        for (t in allTimetable) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("subjectId", t.subjectId)
            obj.put("dayOfWeek", t.dayOfWeek)
            obj.put("startMinute", t.startMinute)
            obj.put("endMinute", t.endMinute)
            obj.put("location", t.location)
            obj.put("attendanceUnitPolicy", t.attendanceUnitPolicy)
            obj.put("notes", t.notes)
            obj.put("createdAt", t.createdAt)
            obj.put("updatedAt", t.updatedAt)
            ttJson.put(obj)
        }
        root.put("timetable_sessions", ttJson)

        val csJson = JSONArray()
        val allClasses = database.openHelper.readableDatabase.query("SELECT * FROM class_sessions").use { cursor ->
            val list = mutableListOf<ClassSessionEntity>()
            while (cursor.moveToNext()) {
                list.add(
                    ClassSessionEntity(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        subjectId = cursor.getLong(cursor.getColumnIndexOrThrow("subjectId")),
                        timetableSessionId = if (cursor.isNull(cursor.getColumnIndexOrThrow("timetableSessionId"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("timetableSessionId")),
                        date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                        startMinute = cursor.getInt(cursor.getColumnIndexOrThrow("startMinute")),
                        endMinute = cursor.getInt(cursor.getColumnIndexOrThrow("endMinute")),
                        location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
                        status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        attendanceUnits = cursor.getInt(cursor.getColumnIndexOrThrow("attendanceUnits")),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
                    )
                )
            }
            list
        }

        for (c in allClasses) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("subjectId", c.subjectId)
            obj.put("timetableSessionId", c.timetableSessionId ?: JSONObject.NULL)
            obj.put("date", c.date)
            obj.put("startMinute", c.startMinute)
            obj.put("endMinute", c.endMinute)
            obj.put("location", c.location)
            obj.put("status", c.status)
            obj.put("attendanceUnits", c.attendanceUnits)
            obj.put("notes", c.notes)
            obj.put("createdAt", c.createdAt)
            obj.put("updatedAt", c.updatedAt)
            csJson.put(obj)
        }
        root.put("class_sessions", csJson)

        return root.toString(2)
    }

    override suspend fun validateBackupJson(jsonString: String): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 0)
            if (version < 1) {
                return BackupValidationResult(false, 0, 0, 0, 0, "Invalid backup version")
            }

            val subjectsArr = root.optJSONArray("subjects") ?: JSONArray()
            val ttArr = root.optJSONArray("timetable_sessions") ?: JSONArray()
            val csArr = root.optJSONArray("class_sessions") ?: JSONArray()

            BackupValidationResult(
                isValid = true,
                version = version,
                subjectCount = subjectsArr.length(),
                timetableSessionCount = ttArr.length(),
                classSessionCount = csArr.length()
            )
        } catch (e: Exception) {
            BackupValidationResult(false, 0, 0, 0, 0, "Failed to parse JSON: ${e.message}")
        }
    }

    override suspend fun restoreBackupJson(jsonString: String, replaceExisting: Boolean): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val subjectsArr = root.optJSONArray("subjects") ?: JSONArray()
            val ttArr = root.optJSONArray("timetable_sessions") ?: JSONArray()
            val csArr = root.optJSONArray("class_sessions") ?: JSONArray()

            database.withTransaction {
                if (replaceExisting) {
                    classSessionDao.deleteAllSessions()
                    timetableDao.deleteAllSessions()
                    subjectDao.deleteAllSubjects()
                }

                // Map old subjectId -> new subjectId
                val subjectIdMap = mutableMapOf<Long, Long>()

                for (i in 0 until subjectsArr.length()) {
                    val obj = subjectsArr.getJSONObject(i)
                    val oldId = obj.getLong("id")
                    val entity = SubjectEntity(
                        id = 0, // auto-generate
                        name = obj.getString("name"),
                        shortCode = obj.optString("shortCode", ""),
                        colorKey = obj.optString("colorKey", "EMERALD"),
                        targetPercentage = obj.optInt("targetPercentage", 75),
                        location = obj.optString("location", ""),
                        semesterStartDate = if (obj.isNull("semesterStartDate")) null else obj.getString("semesterStartDate"),
                        semesterEndDate = if (obj.isNull("semesterEndDate")) null else obj.getString("semesterEndDate"),
                        initialPresent = obj.optInt("initialPresent", 0),
                        initialAbsent = obj.optInt("initialAbsent", 0),
                        isArchived = obj.optBoolean("isArchived", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    val newId = subjectDao.insertSubject(entity)
                    subjectIdMap[oldId] = newId
                }

                // Map old timetableId -> new timetableId
                val ttIdMap = mutableMapOf<Long, Long>()

                for (i in 0 until ttArr.length()) {
                    val obj = ttArr.getJSONObject(i)
                    val oldId = obj.getLong("id")
                    val oldSubjectId = obj.getLong("subjectId")
                    val newSubjectId = subjectIdMap[oldSubjectId] ?: continue

                    val entity = TimetableSessionEntity(
                        id = 0,
                        subjectId = newSubjectId,
                        dayOfWeek = obj.getInt("dayOfWeek"),
                        startMinute = obj.getInt("startMinute"),
                        endMinute = obj.getInt("endMinute"),
                        location = obj.optString("location", ""),
                        attendanceUnitPolicy = obj.optString("attendanceUnitPolicy", "ONE_PER_SESSION"),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    val newId = timetableDao.insertSession(entity)
                    ttIdMap[oldId] = newId
                }

                for (i in 0 until csArr.length()) {
                    val obj = csArr.getJSONObject(i)
                    val oldSubjectId = obj.getLong("subjectId")
                    val newSubjectId = subjectIdMap[oldSubjectId] ?: continue
                    val oldTtId = if (obj.isNull("timetableSessionId")) null else obj.getLong("timetableSessionId")
                    val newTtId = oldTtId?.let { ttIdMap[it] }

                    val entity = ClassSessionEntity(
                        id = 0,
                        subjectId = newSubjectId,
                        timetableSessionId = newTtId,
                        date = obj.getString("date"),
                        startMinute = obj.getInt("startMinute"),
                        endMinute = obj.getInt("endMinute"),
                        location = obj.optString("location", ""),
                        status = obj.optString("status", "PRESENT"),
                        attendanceUnits = obj.optInt("attendanceUnits", 1),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    classSessionDao.insertSession(entity)
                }
            }
            WidgetUpdater.notifyDataChanged(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun exportAttendanceCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Day,Subject,Start Time,End Time,Location,Status,Units,Notes")

        val allSessions = database.openHelper.readableDatabase.query(
            "SELECT c.*, s.name as subjectName FROM class_sessions c LEFT JOIN subjects s ON c.subjectId = s.id ORDER BY c.date DESC, c.startMinute ASC"
        ).use { cursor ->
            val list = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val dateStr = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                val localDate = runCatching { LocalDate.parse(dateStr) }.getOrNull()
                val dayName = localDate?.dayOfWeek?.name ?: ""
                val subjectName = cursor.getString(cursor.getColumnIndexOrThrow("subjectName")) ?: "Unknown"
                val start = TimeUtils.minutesToTimeString(cursor.getInt(cursor.getColumnIndexOrThrow("startMinute")), TimeFormat.FORMAT_24H)
                val end = TimeUtils.minutesToTimeString(cursor.getInt(cursor.getColumnIndexOrThrow("endMinute")), TimeFormat.FORMAT_24H)
                val location = cursor.getString(cursor.getColumnIndexOrThrow("location")) ?: ""
                val status = cursor.getString(cursor.getColumnIndexOrThrow("status")) ?: ""
                val units = cursor.getInt(cursor.getColumnIndexOrThrow("attendanceUnits"))
                val notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""

                // Escape commas, quotes, and newlines per RFC 4180
                fun esc(s: String) = if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) "\"${s.replace("\"", "\"\"")}\"" else s

                list.add("${esc(dateStr)},${esc(dayName)},${esc(subjectName)},${esc(start)},${esc(end)},${esc(location)},${esc(status)},$units,${esc(notes)}")
            }
            list
        }

        for (line in allSessions) {
            sb.appendLine(line)
        }
        return sb.toString()
    }

    override suspend fun resetAllData() {
        database.withTransaction {
            classSessionDao.deleteAllSessions()
            timetableDao.deleteAllSessions()
            subjectDao.deleteAllSubjects()
        }
        WidgetUpdater.notifyDataChanged(context)
    }
}
