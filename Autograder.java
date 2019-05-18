import java.util.List;
import java.util.ArrayList;
import jh61b.grader.TestResult; 
import java.util.Scanner;  //to read in file of diff results
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.io.FileReader;
import java.lang.ProcessBuilder.Redirect;
import org.junit.runner.notification.RunListener;
import org.junit.runner.JUnitCore;
import java.awt.Color;

/**
   Classs representing an autograder.
   It's main method is the running of the autograder
   and instances can be made to store all important information.
*/
public class Autograder extends RunListener {

   /**The list of all tests performed.*/
   private List<TestResult> allTestResults;
   
   /**The current junit test.*/
   private TestResult currentJunitTestResult;

   /** The value of each test.*/
   private final double maxScore = 0.0;
   
   /**The current test number we are on.*/
   private int diffNum;
   
   /**
      The main class constructor.
      Initializes the list of all tests.
   */
   public Autograder() {
      this.allTestResults = new ArrayList<TestResult>();
      this.diffNum = 1;
   }
   
   
   /** Code to run at the end of test run. 
       @throws Exception fails to create json for a test 
    */
   public void testRunFinished() throws Exception {  
      /* Dump allTestResults to StdOut in JSON format. */
      ArrayList<String> objects = new ArrayList<String>();
      for (TestResult tr : this.allTestResults) {
         objects.add(tr.toJSON());
      }
      String testsJSON = String.join(",", objects);
      
      System.out.println("{" + String.join(",", new String[] {
               String.format("\"tests\": [%s]", testsJSON)}) + "}");
   }

   /**
    * Check if source file exists.
    * @param programName the program name
    * @return whether or not the source exists
    */
   public boolean testSourceExists(String programName) {
      boolean sourceExists = false;
   
      File source;
      if (programName.indexOf(".") == -1) {
         source = new File(programName + ".java");
      } else {
         source = new File(programName);
      }
      TestResult trSourceFile = new TestResult(programName +
                                               " Source File Exists", 
                                               "Pre-Test",
                                               this.maxScore, 
                                               "visible");
      
      if (!source.exists() || source.isDirectory()) { // source not present
         trSourceFile.setScore(0);
         trSourceFile.addOutput("ERROR: file " + programName +
                                 ".java is not present!\n");
         trSourceFile.addOutput("\tCheck the spelling of your file name.\n");
      } else { // source present
         trSourceFile.setScore(this.maxScore);
         trSourceFile.addOutput("SUCCESS: file " + programName +
                                 ".java is present!\n");
         sourceExists = true;
      }
      this.allTestResults.add(trSourceFile);
      return sourceExists;
   }

   /** Function to test if a class compiles.
       @param programName the name of the java file to test
       @return whether the class compiled
    */
   public boolean testCompiles(String programName, boolean save) {
      boolean passed = false;
      //File source = new File(programName + ".class");
      TestResult trCompilation = new TestResult(programName + " Compiles",
                                                 "Pre-Test", this.maxScore,
                                                "hidden");
      String fileName = programName + ".java";
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      int compilationResult = compiler.run(null, null, null, fileName);
      if (compilationResult != 0) {
         trCompilation.setScore(0);
         trCompilation.addOutput("ERROR: " + programName + 
                                  ".java did not compile!\n");
         trCompilation.addOutput("\tFix your code and re-submit!");      
      } 
      else {
         trCompilation.setScore(this.maxScore);
         trCompilation.addOutput("SUCCESS: " + programName + 
                                  ".java compiled successfully!\n");
         passed = true;
      }
      if (save) {
         this.allTestResults.add(trCompilation);
      }
      return passed;
   }
   
   
   /**
    * Checks if checkstyle passed.
    * @param programName the program name
    */
   public void testCheckstyle(String programName) {
      TestResult trCheck = new TestResult(programName + "Checkstyle Compliant",
                                          "Pre-Test",
                                           this.maxScore, "hidden");
      String checkstyle = "/autograder/source/checkstyle/";
      
      String result;
      try {
         String proc = "java -jar " + checkstyle + "checkstyle-8.10.1-all.jar" +
            " -c " + checkstyle + "check112.xml /autograder/source/" +
            programName + ".java";
         Process check = Runtime.getRuntime().exec(proc);
         check.waitFor();  
         Scanner s = new Scanner(check.getInputStream()).useDelimiter("\\A");
         result = s.hasNext() ? s.next() : "";
         //no problems reported in checkstylefile; it passed checkstyle
         if (result.equals("Starting audit...\nAudit done.\n")) {
            trCheck.setScore(this.maxScore);
            trCheck.addOutput("SUCCESS: " + programName +
                               " passed checkstyle with no warnings\n");
         }
         else {  //something in checkstylefile; it failed checkstyle
            trCheck.setScore(0);
            trCheck.addOutput("ERROR: " + programName +
                               " did not pass checkstyle." + 
                              " Results are below:\n" + result);
         }
         
      }  catch (IOException e) {
         return;
      } catch (InterruptedException e) {
         return;
      }
      this.allTestResults.add(trCheck);
   }

   
   /**
      Runs a picture diff tests for a specific file.
      All input files are named: {Program_Name}_Diff_#.in
      @param p the program to do diff tests on
      @param prefix the initial part of the name
      @param i the map number
      @param j the image number for the map
      @return whether the pictures match
   */
   public int pictureDiffTest(String p, 
                                  String prefix, 
                                  int i, 
                                  int j) {
      
      String sampleName = prefix + "sample_" + i;
      String resultName = prefix + i;
      if (j >= 0) {
         sampleName += "_" + j + ".png";
         resultName += "_" + j + ".png";
      } else {
         sampleName += ".png";
         resultName += ".png";
      }
      TestResult trDiff = new TestResult(p + 
                                         " Picture Diff Test for "
                                         + resultName + 
                                         " with map # " + i,
                                            "" +  this.diffNum,
                                            this.maxScore, "hidden");
      this.diffNum++;
      int faliure = 0;
   
   
      File fSample = new File(sampleName);
      File fUser = new File(resultName);
      if (fSample.exists() && fUser.exists()) {
         Picture sample = new Picture(sampleName);
         Picture result = new Picture(resultName);
         int[] comp = sample.compare(result, 5);
         if (comp[0] != -1) {
            trDiff.setScore(0);
            trDiff.addOutput("Falied on index: " 
                             + comp[0] + ", " + 
                             comp[1] + " With a difference of: "
                             + comp[2]);
            Color sampleColor = sample.get(comp[0], comp[1]);
            Color resultColor = result.get(comp[0], comp[1]);
            trDiff.addOutput("\n Sample Pixel Value: R = " 
                             + sampleColor.getRed() +
                             ", G = " + sampleColor.getGreen() 
                             + ", B =  " + sampleColor.getBlue());
            trDiff.addOutput("\n Result Pixel Value: R = " 
                             + resultColor.getRed() + ", G = " 
                             + resultColor.getGreen() + ", B =  " 
                             + resultColor.getBlue());
            faliure = 1;
         }
      }
         
      if (fSample.exists() && !fUser.exists()) {
         trDiff.setScore(0);
         trDiff.addOutput(resultName + " is missing.");
      } else if (!fSample.exists() && fUser.exists()) {
         trDiff.setScore(0);
         trDiff.addOutput("The User Run has extra picutres.");
      } else if (faliure == 0) {
         trDiff.setScore(this.maxScore);
         trDiff.addOutput(sampleName + " & " + resultName + " Match.");
      }
      if (fSample.exists() || fUser.exists()) {
         this.allTestResults.add(trDiff);
         return faliure;
      }
      return -1;
   }

   /**
      Runs a all the diff tests for a specific file.
      All input files are named: {Program_Name}_Test_#.in
      @param p the program to do diff tests on
      @param sampleFile true if using a sample program false if just comparing to a file.
   */
   public void diffTests(String name, int count,  boolean sampleFile) {
      PrintStream originalOut = System.out;
      InputStream originalIn = System.in;
      for (int i = 0; i < count; i++) {
         String visible = "hidden";
         if (i == 0) {
            visible = "hidden";
         }
         TestResult trDiff = new TestResult(name + " Diff Test #" + i,
                                            "" + this.diffNum,
                                            this.maxScore, visible);
         this.diffNum++;
         String input = name + "_Diff_" + i + ".in";
         String exOut = name + "_expected_" + i + ".out";
         String acOut = name + "_" + i + ".out";
         String result;
         try {
            File exfile = new File(exOut);
            File infile = new File(input);
            File acfile = new File(acOut);
            File sample = new File(name + "Sample.java");
            if (sampleFile && sample.exists() && !sample.isDirectory()) {
               String[] procSample = {"java", name + "Sample"};
               ProcessBuilder pbSample = new ProcessBuilder(procSample);
               pbSample.redirectOutput(Redirect.to(exfile));
               pbSample.redirectInput(Redirect.from(infile));
               Process sampleProcess = pbSample.start();
               sampleProcess.waitFor();
            }
            System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(acOut))));
            System.setIn(new FileInputStream(input));
            Class<?> act = Class.forName(name);
            if (act == null) {
               throw new ClassNotFoundException();
            }
            Method main = act.getMethod("main", String[].class);
            if (main == null) {
               throw new NoSuchMethodException();
            }
            main.invoke(null);
            String[] procDiff = {"diff", exOut, acOut, "-y", 
                                 "--width=175", "-t" };
            ProcessBuilder pbDiff = new ProcessBuilder(procDiff);
            Process diffProcess = pbDiff.start();
            diffProcess.waitFor();
            Scanner s = new Scanner(diffProcess.getInputStream())
               .useDelimiter("\\A");
            result = s.hasNext() ? s.next() : "";
            
            if (diffProcess.exitValue() == 0) {
               trDiff.setScore(this.maxScore);
               trDiff.addOutput("SUCCESS: " + name +
                                  " passed this diff test\n");
            }
            else { 
               trDiff.setScore(0);
               trDiff.addOutput("ERROR: " + name +
                                  " differed from expected output." +
                                " Results are below:\n" + result);
            } 
            
         }  catch (IOException e) {
            trDiff.setScore(0);
            trDiff.addOutput("ERROR: " + name + 
                             " could not be found to run Diff Test");
         } catch (InterruptedException e) {
            trDiff.setScore(0);
            trDiff.addOutput("ERROR: " +  name +
                             " got interrupted");
         } catch (ClassNotFoundException e) {
            trDiff.setScore(0);
            trDiff.addOutput("ERROR: " + name + 
                             " could not be found to run Diff Test");
         } catch (NoSuchMethodException e) {
            trDiff.setScore(0);
            trDiff.addOutput("ERROR: Students " +
                             name + " Main method not found");
         } catch (IllegalAccessException e) {
            trDiff.setScore(0);
            trDiff.addOutput("ERROR: Students code not accessible");
         } catch (InvocationTargetException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            Throwable et = e.getCause();
            Exception es;
            if(et instanceof Exception) {
               es = (Exception) et;
            } else {
               es = e;
            }
            es.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            trDiff.setScore(0);
            trDiff.addOutput("ERROR: Students code threw " + 
                             e + "\n Stack Trace: " +
                             sStackTrace);
         }
         this.allTestResults.add(trDiff);
         System.setOut(originalOut);
         System.setIn(originalIn);
      }
   }

   public void compTest(String programName, Method m, int ret, Object caller, Object... args) {
      Integer i = ret;
      this.compTest(programName, m, i, caller, args);
   }

   public void compTest(String programName, Method m, boolean ret, Object caller, Object... args) {
      Boolean i = ret;
      this.compTest(programName, m, i, caller, args);
   }

   public void compTest(String programName, Method m, char ret, Object caller, Object... args) {
      Character i = ret;
      this.compTest(programName, m, i, caller, args);
   }

   public void compTest(String programName, Method m, double ret, Object caller, Object... args) {
      Double i = ret;
      this.compTest(programName, m, i, caller, args);
   }

   public void compTest(String programName, Method m, Object ret, Object caller, Object... args) {
      TestResult trComp = new TestResult(programName + " Unit Test # " + this.diffNum,
                                         "" + this.diffNum,
                                         this.maxScore, "hidden");
      this.diffNum++;
      if (m != null) {
         try {
            Object t = m.invoke(caller, args);
            if (t.equals(ret)) {
               trComp.setScore(this.maxScore);
               trComp.addOutput("SUCCESS: Method - "
                                + m.getName() + 
                                " Returned the correct output of: " + 
                                ret + " On Inputs: \n");

            } else {
               trComp.setScore(0);
               trComp.addOutput("FALIURE: Method - "
                                + m.getName() + 
                                " Returned the incorrect output of: " + 
                                t + " instead of " + ret + " On Inputs: \n");
            }
            for (Object arg: args) {
               trComp.addOutput("" + arg + "\n");
            }
         } catch (IllegalAccessException e) {
            trComp.setScore(0);
            trComp.addOutput("ERROR: Method - " + 
                             m.getName() + "Is not Accessible");
         } catch (InvocationTargetException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            Throwable et = e.getCause();
            Exception es;
            if(et instanceof Exception) {
               es = (Exception) et;
            } else {
               es = e;
            }
            es.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            trComp.setScore(0);
            trComp.addOutput("ERROR: Method = " +
                             m.getName() +
                             " threw " + 
                             es + "On Inputs: \n");
            for (Object arg: args) {
               trComp.addOutput("" + arg + "\n");
            }
            trComp.addOutput("\n Stack Trace: " +
                             sStackTrace);
         }
      } else {
         trComp.setScore(0);
            trComp.addOutput("ERROR: Method - " + 
                             m.getName() + "Does not exist");
      }
   }


   public static Method getMethod(String programName,
                                  String methodName, 
                                  Class<?>... paramTypes) {
      try {
         Class<?> c = Class.forName(programName);
         if (c == null) {
            throw new ClassNotFoundException();
         }
         Method m = c.getMethod(methodName, paramTypes);
         return m;
      } catch(Exception e) {
         return null;
      }

   }

   /**
      Runs a all the diff tests for a specific file.
      All input files are named: {Program_Name}_Test_#.in
      @param programName the program to do comparison tests on
      @param testCount the number of tests to perform
   */
   public void comparisonTests(String programName, int testCount) {
      PrintStream original = System.out;
      System.setOut(new PrintStream(
         new OutputStream() {
            public void write(int b) {
            
            }
         }));
      for (int i = 0; i < testCount; i++) {
         TestResult trDiff = new TestResult(programName + " Unit Test #" + i,
                                            "" + this.diffNum,
                                            this.maxScore, "hidden");
         this.diffNum++;
         String input = programName + "_Comp_" + i + ".in";
         String result;
         Scanner s;
         try {
            s = new Scanner(new FileReader(input));
            String method = s.next();
            int argsCount = s.nextInt();
            Class<?>[] ins = new Class<?>[argsCount];
            String option = "1";
            for (int j = 0; j < argsCount; j++) {
               String inputop = s.next();
               Class<?> c;
               switch (inputop) {
                  case "int":
                     c = int.class;
                     break;
                  case "boolean":
                     c = boolean.class;
                     break;
                  case "char":
                     c = char.class;
                     break;
                  case "float":
                     c = float.class;
                     break;
                  default:
                     c  = Class.forName(inputop);
               }
            
               ins[j] = c;
            }
            s.nextLine();
            Object[] args = new Object[argsCount];
            for (int j = 0; j < args.length; j++) {
               Object c;
               String val = s.nextLine();
               if (!ins[j].equals(String.class)) {
                  if (ins[j].equals(int.class)) {
                     c = Integer.parseInt(val);
                  } else if (ins[j].equals(boolean.class)) {
                     c = Boolean.parseBoolean(val);
                  } else if (ins[j].equals(char.class)) {
                     c = val.charAt(0);
                  } else if (ins[j].equals(float.class)) {
                     c = Float.parseFloat(val); 
                  } else {
                     c  = ins[j].cast(val);
                  }
               } else {
                  c = val;
               }
               args[j] = c;
            }
            Class<?> act = Class.forName(programName);
            Class<?> sample = Class.forName(programName + "Sample");
            if (act != null && sample != null) {
               try {
                  Method m = act.getMethod(method, ins);
                  Method ms = sample.getMethod(method, ins);
                  if (m != null && ms != null) {
                     if (method.equals("validateMessage")) {
                        Boolean studentReturn = (Boolean) m.invoke(null, args);
                        Boolean sampleReturn = (Boolean) ms.invoke(null, args);
                        if (studentReturn.equals(sampleReturn)) {
                           trDiff.setScore(this.maxScore);
                           String extraOutput = "Method " + method + 
                              " created the correct output of: " + 
                              sampleReturn.toString() + "\nOn Input:\n";
                           for (int j = 0; j < args.length; j++) {
                              extraOutput += args[j].toString() + "\n";
                           }
                           trDiff.addOutput(extraOutput);
                        } else {
                           trDiff.setScore(0);
                           String extraOutput = "Method " + method 
                              + " created incorrect output of: " + 
                              studentReturn.toString() + 
                              " \nInstead of: " 
                              + sampleReturn.toString() + "\nOn Input:\n";
                           for (int j = 0; j < args.length; j++) {
                              extraOutput += args[j].toString() + "\n";
                           }
                           trDiff.addOutput(extraOutput);
                        }
                     } else {
                        String studentReturn = (String) m.invoke(null, args);
                        String sampleReturn = (String) ms.invoke(null, args);
                        if (studentReturn.equals(sampleReturn)) {
                           trDiff.setScore(this.maxScore);
                           String extraOutput = "Method " + method 
                              + " created the correct output of: " 
                              + sampleReturn.toString() + "\nOn Input:\n";
                           for (int j = 0; j < args.length; j++) {
                              extraOutput += args[j].toString() + "\n";
                           }
                           trDiff.addOutput(extraOutput);
                        } else {
                           trDiff.setScore(0);
                           String extraOutput = "Method " +
                              method + " created incorrect output of: " 
                              + studentReturn.toString() + 
                              " \nInstead of: " + sampleReturn.toString() 
                              + "\nOn Input:\n";
                           for (int j = 0; j < args.length; j++) {
                              extraOutput += args[j].toString() + "\n";
                           }
                           trDiff.addOutput(extraOutput);
                        }
                     }
                  } else {
                     trDiff.setScore(0);
                     trDiff.addOutput("Either the Sample file or the" +
                                      " submission is missing the tested" +
                                      " method");
                  } 
               
               } catch (NoSuchMethodException e) {
                  trDiff.setScore(0);
                  trDiff.addOutput("Method " + method + " could not be found");
               
               } catch (IllegalArgumentException e) {
                  trDiff.setScore(0);
                  trDiff.addOutput("Blame Brandon");
               }
            } else {
               trDiff.setScore(0);
               trDiff.addOutput("One of the classes could not be found");
            }
            s.close();
         } catch (Exception e) {
            trDiff.setScore(0);
            trDiff.addOutput("The testing code crashed while trying to"
                             + " perform this test " + e.toString());
         }
         this.allTestResults.add(trDiff);
      }
      System.setOut(original);
   }


   /**

    */
   public void junitTests(String programName) {
      PrintStream original = System.out;
      System.setOut(new PrintStream(
         new OutputStream() {
            public void write(int b) {
            
            }
         }));
      String fileName = programName + "Tests.java";
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      int compilationResult = compiler.run(null, null, null, fileName);
      if (compilationResult == 0) {
         try {
            Class<?> clss = Class.forName(programName +"Tests");
            JUnitCore junit = new JUnitCore();
            Listener listen = new Listener(this.maxScore, this.diffNum, programName);
            junit.addListener(listen);
            junit.run(clss);
            this.allTestResults.addAll(listen.allResults());
            this.diffNum = listen.unitNum();
         } catch (Exception e){
            //System.out.println(e);
         }
      } else {
         System.err.println(compilationResult);
      }
      System.setOut(original);
   }

 

   /**
      Runs a test to make sure that the student submitted enough methods.
      @param programName the name of the java class
      @param quantity the number of methods the class needs
      @return whether the class has enough methods
    */
   public boolean testMethodCount(String programName, Integer quantity) {
      boolean passed = false;
      TestResult trMethodCount = new TestResult(programName + " Method Count", 
                                                "" + this.diffNum , this.maxScore, "hidden");
      this.diffNum++;
      try {
         Class<?> act = Class.forName(programName);
         if (act == null) {
            trMethodCount.setScore(0);
            trMethodCount.addOutput("Faliure: Class " + 
                                    programName + " could not be found!");
         } else {
            int count = 0;
            Method[] methods = act.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
               if (Modifier.isPublic(methods[i].getModifiers())) {
                  count++;
               }
            }
            try {
               Method m = act.getMethod("main", String[].class);
               if (m != null) {
                  count--;
               }
            } catch (NoSuchMethodException e) {
               //do nothing
            } catch (SecurityException e) {
               //do nothing
            }
            
            if (count < quantity) {
               trMethodCount.setScore(0);
               trMethodCount.addOutput("Faliure: Class " 
                                       + programName +
                                       " is missing expected methods!");
            } else if (count > quantity) {
               trMethodCount.setScore(0);
               trMethodCount.addOutput("Faliure: Class " 
                                       + programName +
                                       " has unexpected public methods!");
            } else {
               trMethodCount.setScore(this.maxScore);
               trMethodCount.addOutput("SUCCESS: Class " + programName +
                                       " has the correct number of  methods!");
               passed = true;
            }
         }
      } catch (ClassNotFoundException e) {
         trMethodCount.setScore(0);
         trMethodCount.addOutput("Faliure: Class " 
                                 + programName + 
                                 " could not be found!");
      }
      this.allTestResults.add(trMethodCount);
      return passed;
   }


   /**
      Runs a test to make sure that the student submitted enough methods.
      @param programName the name of the java class
      @param quantity the number of methods the class needs
      @return whether the class has enough methods
    */
   public boolean testPublicInstanceVariables(String programName) {
      boolean passed = false;
      TestResult trMethodCount = new TestResult(programName + " Check for Public Instance Variables", 
                                                "" + this.diffNum , this.maxScore, "hidden");
      this.diffNum++;
      try {
         Class<?> act = Class.forName(programName);
         if (act == null) {
            trMethodCount.setScore(0);
            trMethodCount.addOutput("Faliure: Class " + 
                                    programName + " could not be found!");
         } else {
            int count = 0;
            Field[] fields = act.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
               if (Modifier.isPublic(fields[i].getModifiers())) {
                  count++;
               }
            }
            
            if (count > 0) {
               trMethodCount.setScore(0);
               trMethodCount.addOutput("Faliure: Class " 
                                       + programName +
                                       " has public Fields!");
            } else {
               trMethodCount.setScore(this.maxScore);
               trMethodCount.addOutput("SUCCESS: Class " + programName +
                                       " has no public fields!");
               passed = true;
            }
         }
      } catch (ClassNotFoundException e) {
         trMethodCount.setScore(0);
         trMethodCount.addOutput("Faliure: Class " 
                                 + programName + 
                                 " could not be found!");
      }
      this.allTestResults.add(trMethodCount);
      return passed;
   }

   public void addTestResult(String name, boolean score, String extraOutput) {
      double scoreNum = 0.0;
      if (score) {
         scoreNum = this.maxScore;
      }
      TestResult tr = new TestResult(name, ""+this.diffNum, this.maxScore, "hidden");
      tr.setScore(scoreNum);
      tr.addOutput(extraOutput);
      this.allTestResults.add(tr);
   }





}