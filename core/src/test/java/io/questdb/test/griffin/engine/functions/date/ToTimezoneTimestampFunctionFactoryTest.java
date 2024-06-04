/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.test.griffin.engine.functions.date;

import io.questdb.griffin.SqlException;
import io.questdb.test.AbstractCairoTest;
import io.questdb.test.tools.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class ToTimezoneTimestampFunctionFactoryTest extends AbstractCairoTest {

    @Test
    public void testAreaName() throws Exception {
        assertToTimezone("select to_timezone(0, 'Europe/Prague')", "1970-01-01T01:00:00.000000Z\n");
    }

    @Test
    public void testInvalidConstantOffset() throws Exception {
        assertMemoryLeak(() -> {
            try {
                assertExceptionNoLeakCheck("select to_timezone(0, '25:40')");
            } catch (SqlException e) {
                Assert.assertEquals(22, e.getPosition());
                TestUtils.assertContains(e.getFlyweightMessage(), "invalid timezone name");
            }
        });
    }

    @Test
    public void testInvalidConstantTimeZone() throws Exception {
        assertMemoryLeak(() -> {
            try {
                assertExceptionNoLeakCheck("select to_timezone(0, 'UUU')");
            } catch (SqlException e) {
                Assert.assertEquals(22, e.getPosition());
                TestUtils.assertContains(e.getFlyweightMessage(), "invalid timezone name");
            }
        });
    }

    @Test
    public void testNullConstantTimeZone() throws Exception {
        assertMemoryLeak(() -> {
            try {
                assertExceptionNoLeakCheck("select to_timezone(0, null)");
            } catch (SqlException e) {
                Assert.assertEquals(22, e.getPosition());
                TestUtils.assertContains(e.getFlyweightMessage(), "timezone must not be null");
            }
        });
    }

    @Test
    public void testTimeOffset() throws Exception {
        assertToTimezone(
                "select to_timezone(cast('2020-03-12T15:30:00.000000Z' as timestamp), '-07:40')",
                "2020-03-12T07:50:00.000000Z\n"
        );
    }

    @Test
    public void testVarInvalidTimezone() throws Exception {
        assertToTimezone(
                "select to_timezone(cast('2020-03-12T15:30:00.000000Z' as timestamp), zone) from (select 'XU' zone)",
                "2020-03-12T15:30:00.000000Z\n"
        );
    }

    @Test
    public void testVarNullTimezone() throws Exception {
        assertMemoryLeak(() -> {
            try {
                assertExceptionNoLeakCheck("select to_timezone(cast('2020-03-12T15:30:00.000000Z' as timestamp), zone) from (select null zone)");
            } catch (SqlException e) {
                Assert.assertEquals(69, e.getPosition());
                TestUtils.assertContains(e.getFlyweightMessage(), "timezone must not be null");
            }
        });
    }

    @Test
    public void testVarTimezone() throws Exception {
        assertToTimezone(
                "select to_timezone(cast('2020-03-12T15:30:00.000000Z' as timestamp), zone) from (select '-07:40' zone)",
                "2020-03-12T07:50:00.000000Z\n"
        );
    }

    @Test
    public void testZoneName() throws Exception {
        assertToTimezone(
                "select to_timezone(cast('2020-03-12T15:30:00.000000Z' as timestamp), 'PST')",
                "2020-03-12T08:30:00.000000Z\n"
        );
    }

    private void assertToTimezone(String sql, String expected) throws Exception {
        assertMemoryLeak(() -> assertSql("to_timezone\n" + expected, sql));
    }

}