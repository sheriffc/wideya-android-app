package uk.org.cgatechnologies.wideya.common.data.database

import android.content.Context
import android.os.Build
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import net.sqlcipher.database.SupportFactory
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.analysis.AnalysisDao
import uk.org.cgatechnologies.wideya.common.data.CommonDao
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.entities.DistrictOffice
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementDao
import uk.org.cgatechnologies.wideya.school_management.entities.*
import uk.org.cgatechnologies.wideya.teacher_management.entities.*
import uk.org.cgatechnologies.wideya.common.data.entities.*
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.learner_management.LearnerIdSequenceDao
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementDao
import uk.org.cgatechnologies.wideya.learner_management.entities.Learner
import uk.org.cgatechnologies.wideya.learner_management.entities.LearnerIdSequence
import uk.org.cgatechnologies.wideya.school_group_management.SchoolGroupManagementDao
import uk.org.cgatechnologies.wideya.school_group_management.entities.SchoolGroup
import uk.org.cgatechnologies.wideya.school_management.PersonAttendanceDao
import uk.org.cgatechnologies.wideya.school_management.SchoolFeedingDao
import uk.org.cgatechnologies.wideya.school_management.SchoolFeedingStockDao
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeeding
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeedingStock
import uk.org.cgatechnologies.wideya.learner_performance.LearnerPerformanceDao
import uk.org.cgatechnologies.wideya.learner_performance.entities.LearnerPerformance
import uk.org.cgatechnologies.wideya.sync_device.SyncDao
import uk.org.cgatechnologies.wideya.sync_device.entities.LogSync
import uk.org.cgatechnologies.wideya.sync_device.entities.TableStatesBiTables
import uk.org.cgatechnologies.wideya.sync_device.entities.TableStatesUniTables
import uk.org.cgatechnologies.wideya.teacher_management.TeacherManagementDao

private const val DB_NAME = Constants.DB_FILENAME
private const val PASSPHRASE = "tests"

@Database(
    entities = [
        Learner::class,
        LearnerIdSequence::class,

        School::class,
        SchoolAcademicYear::class,
        SchoolGroup::class,
        SchoolLearnerAdmission::class,
        SchoolLearnerEnrolment::class,
        SchoolFeeding::class,
        SchoolFeedingStock::class,
        LearnerPerformance::class,

        Teacher::class,
        TeacherPayroll::class,
        TeacherTimetable::class,

        DistrictOffice::class,
        Geo::class,
        MediaPhoto::class,
        OptionList::class,
        OptionListLink::class,
        Person::class,
        PersonAttendance::class,
        PersonContact::class,
        PersonFingerprint::class,

        LogSync::class,
        LogAudit::class,

        TableStatesUniTables::class,
        TableStatesBiTables::class,
    ],
    version = 11,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ]
    )
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolManagementDao(): SchoolManagementDao
    abstract fun schoolGroupManagementDao(): SchoolGroupManagementDao
    abstract fun teacherManagementDao(): TeacherManagementDao
    abstract fun learnerManagementDao(): LearnerManagementDao
    abstract fun learnerIdSequenceDao(): LearnerIdSequenceDao
    abstract fun commonDao(): CommonDao
    abstract fun personAttendanceDao(): PersonAttendanceDao
    abstract fun schoolFeedingDao(): SchoolFeedingDao
    abstract fun schoolFeedingStockDao(): SchoolFeedingStockDao
    abstract fun syncDao(): SyncDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun learnerPerformanceDao(): LearnerPerformanceDao


    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        // teacher_payroll.pin made nullable to support non-payroll teachers (pin = NULL from TSCTMIS)
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `school_feeding` (
                        `uuid` TEXT NOT NULL,
                        `school_uuid` TEXT NOT NULL,
                        `receives_feeding` INTEGER NOT NULL DEFAULT 0,
                        `supply_period_oid` TEXT,
                        `received_at` TEXT,
                        `supplied_by_oid` TEXT,
                        `supplied_by_other` TEXT,
                        `qty_rice` INTEGER,
                        `qty_beans` INTEGER,
                        `qty_gari` INTEGER,
                        `qty_veg_oil` INTEGER,
                        `qty_salt` INTEGER,
                        `created_at` TEXT NOT NULL,
                        `created_by` INTEGER NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `updated_by` INTEGER NOT NULL,
                        `deleted_at` TEXT,
                        `deleted_by` INTEGER,
                        `sync_flag` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`uuid`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_school_feeding_school_uuid` ON `school_feeding` (`school_uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_school_feeding_sync_flag` ON `school_feeding` (`sync_flag`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_school_feeding_updated_at` ON `school_feeding` (`updated_at`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `school` ADD COLUMN `classrooms_oid` TEXT")
                db.execSQL("ALTER TABLE `school` ADD COLUMN `wash_oids` TEXT")
                db.execSQL("ALTER TABLE `school` ADD COLUMN `electricity_oids` TEXT")
                db.execSQL("ALTER TABLE `school` ADD COLUMN `mno_oids` TEXT")
                db.execSQL("ALTER TABLE `school` ADD COLUMN `learning_materials_oids` TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `school_feeding_stock` (
                        `uuid` TEXT NOT NULL,
                        `school_uuid` TEXT NOT NULL,
                        `stock_month` TEXT NOT NULL,
                        `qty_rice` INTEGER,
                        `qty_beans` INTEGER,
                        `qty_gari` INTEGER,
                        `qty_veg_oil` INTEGER,
                        `qty_salt` INTEGER,
                        `created_at` TEXT NOT NULL,
                        `created_by` INTEGER NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `updated_by` INTEGER NOT NULL,
                        `deleted_at` TEXT,
                        `deleted_by` INTEGER,
                        `sync_flag` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`uuid`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_school_feeding_stock_school_uuid` ON `school_feeding_stock` (`school_uuid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_school_feeding_stock_school_uuid_stock_month` ON `school_feeding_stock` (`school_uuid`, `stock_month`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_school_feeding_stock_sync_flag` ON `school_feeding_stock` (`sync_flag`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_school_feeding_stock_updated_at` ON `school_feeding_stock` (`updated_at`)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `school` ADD COLUMN `receives_feeding` INTEGER")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `learner_performance` (
                        `uuid` TEXT NOT NULL,
                        `school_uuid` TEXT NOT NULL,
                        `school_group_uuid` TEXT NOT NULL,
                        `learner_uuid` TEXT NOT NULL,
                        `subject_oid` TEXT NOT NULL,
                        `academic_year` INTEGER NOT NULL,
                        `term_oid` TEXT NOT NULL,
                        `assessment_1_score` REAL,
                        `assessment_2_score` REAL,
                        `max_score` REAL NOT NULL DEFAULT 100.0,
                        `created_at` TEXT NOT NULL,
                        `created_by` INTEGER NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `updated_by` INTEGER NOT NULL,
                        `deleted_at` TEXT,
                        `deleted_by` INTEGER,
                        `sync_flag` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`uuid`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learner_performance_school_uuid` ON `learner_performance` (`school_uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learner_performance_school_group_uuid` ON `learner_performance` (`school_group_uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learner_performance_learner_uuid` ON `learner_performance` (`learner_uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learner_performance_subject_oid` ON `learner_performance` (`subject_oid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learner_performance_updated_at` ON `learner_performance` (`updated_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learner_performance_sync_flag` ON `learner_performance` (`sync_flag`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `teacher_payroll_new` (
                        `uuid` TEXT NOT NULL,
                        `school_sid` INTEGER,
                        `school_emis_id` TEXT,
                        `first_name` TEXT NOT NULL,
                        `middle_name` TEXT,
                        `last_name` TEXT NOT NULL,
                        `sex` TEXT,
                        `date_of_birth` TEXT,
                        `pin` TEXT,
                        `nin` TEXT,
                        `nassit_number` TEXT,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        PRIMARY KEY(`uuid`)
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `teacher_payroll_new` SELECT * FROM `teacher_payroll`")
                db.execSQL("DROP TABLE `teacher_payroll`")
                db.execSQL("ALTER TABLE `teacher_payroll_new` RENAME TO `teacher_payroll`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_teacher_payroll_pin` ON `teacher_payroll` (`pin`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_teacher_payroll_updated_at` ON `teacher_payroll` (`updated_at`)")
            }
        }

        fun getInstance(): AppDatabase?{
            return instance
        }

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): AppDatabase =
            instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .apply {
                        when (BuildConfig.BUILD_TYPE) {
//                            "release" -> {
//                                this.openHelperFactory(SupportFactory(Utils.getPassphrase(context)))
//                            }
                            "pilot" -> this.createFromAsset("database/wideya_seed_prepilot.db")
                            else -> {}
                        }
                    }
//                .fallbackToDestructiveMigration()
                .build()

                this.instance = instance

                instance
            }
    }
}
