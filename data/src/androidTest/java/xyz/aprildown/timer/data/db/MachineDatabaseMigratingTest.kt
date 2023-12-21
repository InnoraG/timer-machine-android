package xyz.aprildown.timer.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MachineDatabaseMigratingTest {

    @get:Rule
    val testHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MachineDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun test() {
        val currentVersion = MachineDatabase.DB_VERSION
        for (fromNth in 1..currentVersion) {
            for (toNth in fromNth..currentVersion) {
                migrationTest(fromNth, toNth)
            }
        }
    }

    private fun migrationTest(fromNth: Int, toNth: Int) {
        val db = testHelper.createDatabase(MachineDatabase.DB_NAME, fromNth)
        testHelper.runMigrationsAndValidate(
            MachineDatabase.DB_NAME,
            toNth,
            true,
            MachineDatabase.getMigration1to2(),
            MachineDatabase.getMigration2to3(),
            MachineDatabase.getMigration3to4(),
            MachineDatabase.getMigration4to5(),
            MachineDatabase.getMigration5to6(),
            MachineDatabase.getMigration6to7(),
            MachineDatabase.getMigration7to8(),
        )
        testHelper.closeWhenFinished(db)
    }
}
