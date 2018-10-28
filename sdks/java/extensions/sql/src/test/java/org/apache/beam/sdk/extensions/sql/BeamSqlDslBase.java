/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.extensions.sql;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.TestStream;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.SerializableFunctions;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * prepare input records to test.
 *
 * <p>Note that, any change in these records would impact tests in this package.
 */
public class BeamSqlDslBase {
  public static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

  @Rule public final TestPipeline pipeline = TestPipeline.create();
  @Rule public ExpectedException exceptions = ExpectedException.none();

  static Schema schemaInTableA;
  static Schema schemaFloatDouble;
  static Schema schemaArray;
  static List<Row> rowsInTableA;
  static List<Row> rowsOfFloatDouble;
  static List<Row> rowsOfArray;

  //bounded PCollections
  protected PCollection<Row> boundedInput1;
  protected PCollection<Row> boundedInput2;
  protected PCollection<Row> boundedInputFloatDouble;
  protected PCollection<Row> boundedInputArray;

  //unbounded PCollections
  protected PCollection<Row> unboundedInput1;
  protected PCollection<Row> unboundedInput2;

  @BeforeClass
  public static void prepareClass() throws ParseException {
    schemaInTableA =
        Schema.builder()
            .addInt32Field("f_int")
            .addInt64Field("f_long")
            .addInt16Field("f_short")
            .addByteField("f_byte")
            .addFloatField("f_float")
            .addDoubleField("f_double")
            .addStringField("f_string")
            .addDateTimeField("f_timestamp")
            .addInt32Field("f_int2")
            .addDecimalField("f_decimal")
            .build();

    rowsInTableA =
        TestUtils.RowsBuilder.of(schemaInTableA)
            .addRows(
                1,
                1000L,
                (short) 1,
                (byte) 1,
                1.0f,
                1.0d,
                "string_row1",
                FORMAT.parseDateTime("2017-01-01 01:01:03"),
                0,
                new BigDecimal(1))
            .addRows(
                2,
                2000L,
                (short) 2,
                (byte) 2,
                2.0f,
                2.0d,
                "string_row2",
                FORMAT.parseDateTime("2017-01-01 01:02:03"),
                0,
                new BigDecimal(2))
            .addRows(
                3,
                3000L,
                (short) 3,
                (byte) 3,
                3.0f,
                3.0d,
                "string_row3",
                FORMAT.parseDateTime("2017-01-01 01:06:03"),
                0,
                new BigDecimal(3))
            .addRows(
                4,
                4000L,
                (short) 4,
                (byte) 4,
                4.0f,
                4.0d,
                "第四行",
                FORMAT.parseDateTime("2017-01-01 02:04:03"),
                0,
                new BigDecimal(4))
            .getRows();

    //    schemaFloatDouble =         Schema.builder()
    ////            .addNullableField("f_int", Schema.FieldType.INT32)
    ////            .addNullableField("f_long", Schema.FieldType.INT64)
    ////            .addNullableField("f_short", Schema.FieldType.INT16)
    ////            .addNullableField("f_byte", Schema.FieldType.BYTE)
    ////            .addNullableField("f_float", Schema.FieldType.FLOAT)
    ////            .addNullableField("f_double", Schema.FieldType.DOUBLE)
    ////            .addNullableField("f_string", Schema.FieldType.STRING)
    ////            .addNullableField("f_timestamp", Schema.FieldType.DATETIME)
    ////            .build();

    schemaFloatDouble =
        Schema.builder()
            .addFloatField("f_float_1")
            .addDoubleField("f_double_1")
            .addFloatField("f_float_2")
            .addDoubleField("f_double_2")
            .addFloatField("f_float_3")
            .addDoubleField("f_double_3")
            .build();

    rowsOfFloatDouble =
        TestUtils.RowsBuilder.of(schemaFloatDouble)
            .addRows(
                Float.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Float.NaN,
                Double.NaN)
            .getRows();

//    schemaArray =
//        Schema.builder().addInt32Field("id").addNullableField("arr", Schema.FieldType.array(Schema.FieldType.DOUBLE)).build();
//    rowsOfArray =
//        TestUtils.RowsBuilder.of(schemaArray)
//            .addRows(
//                1,
//                ImmutableList.of(1.0, 2.0, 3.0, 4.0, 5.0),
//                2,
//                    Arrays.asList(null, 1.0, 2.0),
//                3,
//                ImmutableList.of(1.0, 2.0, Double.NaN),
//                4,
//                ImmutableList.of(100.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
//            .getRows();
  }

  @Before
  public void preparePCollections() {
    boundedInput1 =
        pipeline.apply(
            "boundedInput1",
            Create.of(rowsInTableA)
                .withSchema(
                    schemaInTableA,
                    SerializableFunctions.identity(),
                    SerializableFunctions.identity()));

    boundedInput2 =
        pipeline.apply(
            "boundedInput2",
            Create.of(rowsInTableA.get(0))
                .withSchema(
                    schemaInTableA,
                    SerializableFunctions.identity(),
                    SerializableFunctions.identity()));

    boundedInputFloatDouble =
        pipeline.apply(
            "boundedInputFloatDouble",
            Create.of(rowsOfFloatDouble)
                .withSchema(
                    schemaFloatDouble,
                    SerializableFunctions.identity(),
                    SerializableFunctions.identity()));

//    boundedInputArray =
//        pipeline.apply(
//            "boundedInputArray",
//            Create.of(rowsOfArray)
//                .withSchema(
//                    schemaArray,
//                    SerializableFunctions.identity(),
//                    SerializableFunctions.identity()));

    unboundedInput1 = prepareUnboundedPCollection1();
    unboundedInput2 = prepareUnboundedPCollection2();
  }

  private PCollection<Row> prepareUnboundedPCollection1() {
    TestStream.Builder<Row> values =
        TestStream.create(
            schemaInTableA, SerializableFunctions.identity(), SerializableFunctions.identity());

    for (Row row : rowsInTableA) {
      values = values.advanceWatermarkTo(new Instant(row.getDateTime("f_timestamp")));
      values = values.addElements(row);
    }

    return PBegin.in(pipeline)
        .apply("unboundedInput1", values.advanceWatermarkToInfinity())
        .apply(
            "unboundedInput1.fixedWindow1year",
            Window.into(FixedWindows.of(Duration.standardDays(365))));
  }

  private PCollection<Row> prepareUnboundedPCollection2() {
    TestStream.Builder<Row> values =
        TestStream.create(
            schemaInTableA, SerializableFunctions.identity(), SerializableFunctions.identity());

    Row row = rowsInTableA.get(0);
    values = values.advanceWatermarkTo(new Instant(row.getDateTime("f_timestamp")));
    values = values.addElements(row);

    return PBegin.in(pipeline)
        .apply("unboundedInput2", values.advanceWatermarkToInfinity())
        .apply(
            "unboundedInput2.fixedWindow1year",
            Window.into(FixedWindows.of(Duration.standardDays(365))));
  }
}
