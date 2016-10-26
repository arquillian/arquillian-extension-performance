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

   public double workingHardSeconds() {
       try
       {
          Thread.sleep(1000);
       }
       catch (InterruptedException ie)
       {
          ie.printStackTrace();
       }
       return 21;
   }
}
