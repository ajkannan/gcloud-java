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

import static com.google.datastore.v1beta3.Value.TIMESTAMP_VALUE_FIELD_NUMBER;

public final class DateTimeValue extends Value<DateTime> {

  private static final long serialVersionUID = -5096238337676649540L;

  static final BaseMarshaller<DateTime, DateTimeValue, Builder> MARSHALLER =
      new BaseMarshaller<DateTime, DateTimeValue, Builder>() {

        private static final long serialVersionUID = -5695812592049332840L;

        @Override
        public int getProtoFieldId() {
          return TIMESTAMP_VALUE_FIELD_NUMBER;
        }

        @Override
        public Builder newBuilder(DateTime value) {
          return builder(value);
        }

        @Override
        protected DateTime getValue(com.google.datastore.v1beta3.Value from) {
          long seconds = from.getTimestampValue().getSeconds();
          int nanos = from.getTimestampValue().getNanos();
          return new DateTime(seconds * 1000000 + nanos / 1000);
        }

        @Override
        protected void setValue(DateTimeValue from, com.google.datastore.v1beta3.Value.Builder to) {
          long microseconds = from.get().timestampMicroseconds();
          long seconds = microseconds / 1000000;
          int nanos = (int) (microseconds % 1000) * 1000;
          to.setTimestampValue(com.google.protobuf.Timestamp.newBuilder()
              .setSeconds(seconds).setNanos(nanos));
        }
      };

  public static final class Builder extends Value.BaseBuilder<DateTime, DateTimeValue, Builder> {

    private Builder() {
      super(ValueType.DATE_TIME);
    }

    @Override
    public DateTimeValue build() {
      return new DateTimeValue(this);
    }
  }

  public DateTimeValue(DateTime dateTime) {
    this(builder(dateTime));
  }

  private DateTimeValue(Builder builder) {
    super(builder);
  }

  @Override
  public Builder toBuilder() {
    return new Builder().mergeFrom(this);
  }

  public static DateTimeValue of(DateTime dateTime) {
    return new DateTimeValue(dateTime);
  }

  public static Builder builder(DateTime dateTime) {
    return new Builder().set(dateTime);
  }
}
