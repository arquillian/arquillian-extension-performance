package org.jboss.arquillian.performance.junit;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.performance.HardWorker;
import org.jboss.arquillian.performance.annotation.Performance;
import org.jboss.arquillian.performance.annotation.PerformanceTest;
import org.jboss.arquillian.performance.exception.PerformanceException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@PerformanceTest(resultsThreshold=2)
@RunWith(Arquillian.class)
public class WorkHardTestCase
{

   @Test
   @Performance(time=30)
   public void doHardWork() throws Exception 
   {
      HardWorker worker = new HardWorker();
      assertThat(worker.workingHard(), is(21d));
   }

   @Test
   @Performance(time=2, unit=TimeUnit.SECONDS)
   public void doHardWorkSeconds() throws Exception 
   {
      HardWorker worker = new HardWorker();
      assertThat(worker.workingHardSeconds(), is(21d));
   }

   @Test(expected=PerformanceException.class)
   @Performance(time=10)
   public void doHardWorkWithFail() throws Exception 
   {
      HardWorker worker = new HardWorker();
      assertThat(worker.workingHard(), is(21d));
   }

   /**
    * This method is supposed to fail with @Performance(time=9)
    * 
    * @throws Exception
    */
   @Test(expected = PerformanceException.class)
   @Performance(time=5)
   public void doHardWorkThatFails() throws Exception
   {
      HardWorker worker = new HardWorker();
      Assert.assertEquals(21, worker.workingHard(), 0d);
   }
}
