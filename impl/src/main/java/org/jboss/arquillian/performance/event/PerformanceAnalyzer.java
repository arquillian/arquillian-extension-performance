/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.performance.event;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.performance.annotation.Performance;
import org.jboss.arquillian.performance.exception.PerformanceException;
import org.jboss.arquillian.performance.meta.PerformanceClassResult;
import org.jboss.arquillian.performance.meta.PerformanceMethodResult;
import org.jboss.arquillian.performance.meta.PerformanceSuiteResult;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.Test;

import java.io.*;
import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Compares and stores test durations. 
 *
 * fired during test
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 * @version $Revision: 1.1 $
 */

public class PerformanceAnalyzer
{
	private final String ARQ_PERF_FOLDER = "arq-perf-folder";
    private final String folder = "arq-perf"; 

    private final SimpleDateFormat fileFormat = new SimpleDateFormat("dd.MM.yy.mm.ss");

    @Inject
    private Instance<PerformanceSuiteResult> suiteResultInst;

    @Inject
    private Instance<TestResult> testResultInst;

    public void callback(@Observes EventContext<Test> eventContext) throws Exception
    {
        //first let the test run
        eventContext.proceed();
        // then verify that the test performed within the specified time
        verifyPerformance(eventContext.getEvent());
        // then compare with previous results

        PerformanceSuiteResult suiteResult = suiteResultInst.get();

        if (suiteResult != null)
        {
            try
            {
                comparePerformanceSuiteResults(suiteResult, eventContext.getEvent().getTestMethod().getName());
            }
            catch (PerformanceException pe)
            {
                TestResult result = testResultInst.get();
                if (result != null)
                {
                    result.setThrowable(pe);
                }
            }
        }
    }

    private void comparePerformanceSuiteResults(PerformanceSuiteResult suiteResult, String testMethod)
            throws PerformanceException
    {
        List<PerformanceSuiteResult> prevResults = findEarlierResults(suiteResult);

        for (PerformanceSuiteResult result : prevResults)
        {
            doCompareResults(result, suiteResult, testMethod);
        }

        //everything went well, now we just store the new result and we're done
        storePerformanceSuiteResult(suiteResult);
    }

    private void doCompareResults(PerformanceSuiteResult oldResult, PerformanceSuiteResult newResult, String testMethod)
            throws PerformanceException
    {
        for (String className : oldResult.getResults().keySet())
        {

            PerformanceClassResult oldClassResult = oldResult.getResult(className);
            if (oldClassResult.getMethodResult(testMethod) != null)
            {
                oldClassResult.getMethodResult(testMethod).compareResults(
                        newResult.getResult(className).getMethodResult(testMethod),
                        oldClassResult.getPerformanceSpecs().resultsThreshold());
            }

        }

    }


    private List<PerformanceSuiteResult> findEarlierResults(final PerformanceSuiteResult currentResult)
    {
        File perfDir = getArqPerfFolder();
        File[] files = perfDir.listFiles(new FileFilter()
        {
            public boolean accept(File pathName)
            {
                return (pathName.getName().startsWith(currentResult.getName()));
            }

        });
        List<PerformanceSuiteResult> prevResults = new ArrayList<PerformanceSuiteResult>();
        if (files != null)
        {
            for (File f : files)
            {
                PerformanceSuiteResult result = getResultFromFile(f);
                if (result != null)
                    prevResults.add(result);
            }
        }
        return prevResults;
    }
    
    private File getArqPerfFolder() {
    	
    	String arqPerfFolder = System.getProperty(ARQ_PERF_FOLDER);
    	System.out.println(arqPerfFolder);
    	if (arqPerfFolder == null) {
    		arqPerfFolder = System.getProperty("user.dir");
    	}
    	
    	return new File(arqPerfFolder + File.separator + folder);
    }

    private PerformanceSuiteResult getResultFromFile(File file)
    {
    	FileInputStream fis = null;
    	ObjectInputStream ois = null;
        try
        {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            PerformanceSuiteResult result = (PerformanceSuiteResult) ois.readObject();
            
            return result;
        }
        catch (IOException ioe)
        {
            return null;
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            return null;
        } finally {
        	closeStreamSilently(fis);
        	closeStreamSilently(ois);
        }
    }

	/**
     * 1. make sure folder exists, if not create folder
     * 2. generate file name
     * 3. save file
     *
     * @param suiteResult that should be saved
     */
    private void storePerformanceSuiteResult(PerformanceSuiteResult suiteResult)
    {
        String filename = suiteResult.getName() + "-" + fileFormat.format(new Date()) + ".ser";
        File currentPath = getArqPerfFolder();
        boolean fileStatus = true;
        if (!currentPath.isDirectory())
            fileStatus = currentPath.mkdirs();
        if (fileStatus)
        {
            try
            {
                FileOutputStream fos = new FileOutputStream(currentPath.getPath() + File.separator + filename);
                ObjectOutputStream out = new ObjectOutputStream(fos);
                out.writeObject(suiteResult);
                out.close();
            }
            catch (IOException ex)
            {
                System.err.println("Storing test results failed.");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Verify that the test ended within specified time
     *
     * @param event, the test
     * @throws PerformanceException if the test did not end within the specified time
     */
    private void verifyPerformance(Test event) throws PerformanceException {
        TestResult result = testResultInst.get();
        if(result != null)
        {
            //check if we have set a threshold
            Performance performance = null;
            Annotation[] annotations =  event.getTestMethod().getDeclaredAnnotations();
            for(Annotation a : annotations)
                if(a.annotationType().getName().equals(Performance.class.getCanonicalName()))
                    performance = (Performance) a;
            
            
            double maxTime = -1;
            if(performance != null)
            {
                //System.out.println("For test: "+event.toString()+", it took: "+(result.getEnd()-result.getStart()));
                if(performance.time() > 0 &&
                        performance.time() < (result.getEnd()-result.getStart()))
                {
                    result.setStatus(TestResult.Status.FAILED);
                    result.setThrowable(
                            new PerformanceException("The test didnt finish within the specified time: "
                                    +performance.time()+"ms, it took "+(result.getEnd()-result.getStart())+"ms."));
                }
                
                maxTime = performance.time();
            }
            
            // fetch suiteResult, get the correct classResult and append the test to that
            // classResult.
            PerformanceSuiteResult suiteResult = suiteResultInst.get();
            if(suiteResult != null) {
                suiteResult.getResult(event.getTestClass().getName()).addMethodResult(
                        new PerformanceMethodResult(
                        		maxTime, result.getEnd()-result.getStart(), event.getTestMethod()));
            }
        }
    }
    
    private void closeStreamSilently(InputStream is) {
    	if (is != null) {
    		try {
				is.close();
			} catch (IOException e) {
				
			}
    	}
	}
}
