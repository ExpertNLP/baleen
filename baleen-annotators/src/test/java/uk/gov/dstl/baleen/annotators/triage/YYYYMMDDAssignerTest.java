// Copyright (c) Committed Software 2018, opensource@committed.io
package uk.gov.dstl.baleen.annotators.triage;

import static org.junit.Assert.assertEquals;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Ignore;
import org.junit.Test;

import uk.gov.dstl.baleen.annotators.testing.AbstractAnnotatorTest;

public class YYYYMMDDAssignerTest extends AbstractAnnotatorTest {

  public YYYYMMDDAssignerTest() {
    super(YYYYMMDDAssigner.class);
  }
  // TODO: fix
  @Ignore
  @Test
  public void test() throws Exception {

    getDocumentAnnotation().setSourceUri("/this/is/a/2017/01/23/valid/path/index.html");
    getDocumentAnnotation().setTimestamp(1);

    jCas.setDocumentText("Hello world.");
    processJCas();

    final long timestamp = getDocumentAnnotation().getTimestamp();

    GregorianCalendar gCal = new GregorianCalendar(2017, 0, 23);
    gCal.setTimeZone(TimeZone.getTimeZone("UTC")); // don't use default 'locale' but UTC timezone
    assertEquals(gCal.getTime().getTime(), timestamp);
  }
}
