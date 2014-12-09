package com.google.gcloud.datastore;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.format.ISODateTimeFormat;

import java.util.Calendar;
import java.util.Date;

/**
 * A Google Cloud Datastore timestamp (represented in micro-seconds).
 * This class is immutable.
 *
 * @see <a href="https://cloud.google.com/datastore/docs/concepts/entities">Google Cloud Datastore Entities, Properties, and Keys</a>
 */
public final class DateTime implements java.io.Serializable {

  private static final long serialVersionUID = 7343324797621228378L;

  private final long timestampMicroseconds;

  DateTime(long timestampMicroseconds) {
    this.timestampMicroseconds = timestampMicroseconds;
  }

  @Override
  public String toString() {
    return ISODateTimeFormat.dateTime().print(timestampMillis());
  }

  @Override
  public int hashCode() {
    return (int) timestampMicroseconds;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DateTime)) {
      return false;
    }
    return timestampMicroseconds == ((DateTime) obj).timestampMicroseconds;
  }

  public long timestampMicroseconds() {
    return timestampMicroseconds;
  }

  public long timestampMillis() {
    return timestampMicroseconds / 1000L;
  }

  public Date toDate() {
    return new Date(timestampMillis());
  }

  public Calendar toCalendar() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(timestampMillis());
    return cal;
  }

  public static DateTime now() {
    return new DateTime(System.nanoTime() / 1000L);
  }

  public static DateTime copyFrom(Date date) {
    return new DateTime(checkNotNull(date).getTime() * 1000L);
  }

  public static DateTime copyFrom(Calendar calendar) {
    return copyFrom(calendar.getTime());
  }
}