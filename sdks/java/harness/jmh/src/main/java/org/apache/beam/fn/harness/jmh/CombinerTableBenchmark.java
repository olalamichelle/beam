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
package org.apache.beam.fn.harness.jmh;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.List;
import org.apache.beam.fn.harness.GroupingTable;
import org.apache.beam.fn.harness.PrecombineGroupingTable;
import org.apache.beam.sdk.coders.CoderRegistry;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.coders.VarIntCoder;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.Sum;
import org.apache.beam.sdk.util.WindowedValue;
import org.apache.beam.sdk.values.KV;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

public class CombinerTableBenchmark {
  @State(Scope.Benchmark)
  public static class CombinerTable {
    final int num_keys = 1000;
    final int num_per_key = 1000;
    final Combine.BinaryCombineIntegerFn sumInts = Sum.ofIntegers();
    final PipelineOptions options = PipelineOptionsFactory.create();
    GroupingTable<WindowedValue<String>, Integer, int[]> groupingTable;
    List<WindowedValue<KV<String, Integer>>> elements;

    @Param({"true", "false"})
    public String globallyWindowed;

    @Setup(Level.Invocation)
    public void setUp() {
      groupingTable =
          PrecombineGroupingTable.combiningAndSampling(
              options,
              sumInts,
              StringUtf8Coder.of(),
              sumInts.getAccumulatorCoder(CoderRegistry.createDefault(), VarIntCoder.of()),
              .001,
              Boolean.valueOf(globallyWindowed));
      elements = new ArrayList<>();
      for (int i = 0; i < num_keys; i++) {
        elements.add(WindowedValue.valueInGlobalWindow(KV.of(Integer.toString(i), i)));
      }
    }
  }

  @Benchmark
  @Fork(value = 1)
  @Warmup(time = 1, timeUnit = SECONDS)
  @Measurement(time = 1, timeUnit = SECONDS)
  public void uniformDistribution(CombinerTable table) throws Exception {
    for (int i = 0; i < table.num_per_key; i++) {
      for (WindowedValue<KV<String, Integer>> element : table.elements) {
        table.groupingTable.put(element, null);
      }
    }
  }
}
