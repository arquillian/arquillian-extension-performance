package org.jboss.arquillian.performance;

public class HardWorker
{
   public double workingHard()
   { 
      try
      {
         Thread.sleep(20);
      }
      catch (InterruptedException ie)
      {
         ie.printStackTrace();
      }
      return 21;
   }
}
