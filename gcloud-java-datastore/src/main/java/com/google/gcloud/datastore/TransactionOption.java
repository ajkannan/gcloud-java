/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gcloud.datastore;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.Map;


public abstract class TransactionOption implements Serializable {

  private static final long serialVersionUID = -1862234444015690375L;

  public static final class ForceWrites extends TransactionOption {

    private static final long serialVersionUID = -6873967516988380886L;

    private final boolean force;

    public ForceWrites(boolean force) {
      this.force = force;
    }

    public boolean force() {
      return force;
    }

    @Override
    BatchOption toBatchWriteOption() {
      return new BatchOption.ForceWrites(force);
    }
  }  

  TransactionOption() {
    // package protected
  }

  public static ForceWrites forceWrites() {
    return new ForceWrites(true);
  }

  static Map<Class<? extends TransactionOption>, TransactionOption> asImmutableMap(
      TransactionOption... options) {
    ImmutableMap.Builder<Class<? extends TransactionOption>, TransactionOption> builder =
        ImmutableMap.builder();
    for (TransactionOption option : options) {
      builder.put(option.getClass(), option);
    }
    return builder.build();
  }

  abstract BatchOption toBatchWriteOption();
}
