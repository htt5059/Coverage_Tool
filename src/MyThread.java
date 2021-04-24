import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MyThread extends Thread {

    public MyThread() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    VirtualMachine vm;
    boolean stopOnVMStart;
    boolean connected = true;
    ReferenceType rt;
    HashMap<String, Integer> hm;
    String pkgName;
    Frame mainFrame;

    public MyThread(VirtualMachine vm, boolean stopOnVMStart, String pkgName, HashMap<String, Integer> hm , Frame inputFrame) {
        this.vm = vm;
        this.stopOnVMStart = stopOnVMStart;
        if(hm==null){
            this.hm = new HashMap<String, Integer>();
        }else this.hm = hm;
        this.pkgName = pkgName;
        mainFrame= inputFrame;
        this.start();
    }

    public void run() {
        EventQueue queue = vm.eventQueue();
        for (int i = 1; i <= hm.size(); i++) {
            ClassPrepareRequest cpr = vm.eventRequestManager().createClassPrepareRequest();
            cpr.addClassFilter(pkgName + ".*");
            cpr.addCountFilter(i);
            cpr.enable();
        }

        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                boolean resumeStoppedApp = false;
                EventIterator it = eventSet.eventIterator();
                while (it.hasNext()) {
                    Event e = it.nextEvent();
                    resumeStoppedApp |= !handleEvent(e);
                }
                if (resumeStoppedApp) {
                    eventSet.resume();
                }
            } catch (Exception exc) {
                System.out.println(exc);
            }
        }
    }

    private boolean handleEvent(Event event) {
        if (event instanceof ExceptionEvent) {
            return exceptionEvent(event);
        } else if (event instanceof BreakpointEvent) {
            return breakpointEvent(event);
        } else if (event instanceof WatchpointEvent) {
            return fieldWatchEvent(event);
        } else if (event instanceof StepEvent) {
            return stepEvent(event);
        } else if (event instanceof MethodEntryEvent) {
            return methodEntryEvent(event);
        } else if (event instanceof MethodExitEvent) {
            return methodExitEvent(event);
        } else if (event instanceof ClassPrepareEvent) {
            /*provide code
             The goal is to utilize the event object to identify methods in
             the the reference type class. Find the locations of the methods
             in the code. Use the virtual machine object to set breakpoints
             for each location and enable them.
             */
            //     :
            //     :
            ClassPrepareEvent cpe = (ClassPrepareEvent) event;
            rt=cpe.referenceType();
            try{
                List list = rt.methods();
                Object[] o = list.toArray();
                for(Object i: o){
                    Method m=(Method)i;
                    Location location =m.location();
                    BreakpointRequest breakRequest = vm.eventRequestManager().createBreakpointRequest(location);
                    breakRequest.enable();
                }
            }
            catch(Exception e){
                System.out.println(e);
            }
            return classPrepareEvent(event);
        } else if (event instanceof ClassUnloadEvent) {
            return classUnloadEvent(event);
        } else if (event instanceof ThreadStartEvent) {
            return threadStartEvent(event);
        } else if (event instanceof ThreadDeathEvent) {
            return threadDeathEvent(event);
        } else if (event instanceof VMStartEvent) {
            return vmStartEvent(event);
        } else {
            return handleExitEvent(event);
        }
    }

    private boolean vmDied = false;

    private boolean handleExitEvent(Event event) {
        if (event instanceof VMDeathEvent) {
            vmDied = true;
            return vmDeathEvent(event);
        } else if (event instanceof VMDisconnectEvent) {
            connected = false;
            if (!vmDied) {
                vmDisconnectEvent(event);
            }
            return false;
        } else {
            throw new InternalError("Unexpected event type");
        }
    }

    private boolean vmStartEvent(Event event) {
        VMStartEvent se = (VMStartEvent) event;
        return stopOnVMStart;
    }

    private boolean breakpointEvent(Event event) {
        /* provide code
           This method is called when a break point is encountered.
           Take advantage of this break point event to identify the location.
           The method and class can be identified from the location. Make an
           update to the number of times of execution for the class's method
           in real time.
         */
        //     :
        //     :
        //     :
        BreakpointEvent breakpoint = (BreakpointEvent) event;
        String methodName=breakpoint.location().method().toString().split("\\(")[0];
        int size= methodName.split("\\.").length;
        if(methodName.split("\\.")[size-1].equals("<init>")){
            methodName=methodName.split("\\.")[size-2];
        }else methodName=methodName.split("\\.")[size-1];

        if(hm.containsKey(methodName)){
            int key = hm.get(methodName)+1;
            hm.put(methodName, key);
        }else {
            hm.put(methodName, 0);
        }
        for(Map.Entry hmEntry: hm.entrySet()){
            String key = (String)hmEntry.getKey();
            int val = (int)hmEntry.getValue();
            System.out.println(key+"\t\t"+val);
        }
        return false;
    }

    private boolean methodEntryEvent(Event event) {
        MethodEntryEvent me = (MethodEntryEvent) event;
        System.out.println("MethodEntryEvent");
        System.out.println(me.method().toString());
        System.out.println(me.location().lineNumber());
        return true;
    }
    public HashMap returnHashMap(){ return hm;}
    private boolean methodExitEvent(Event event) {
        MethodExitEvent me = (MethodExitEvent) event;
        return true;
    }

    private boolean fieldWatchEvent(Event event) {
        WatchpointEvent fwe = (WatchpointEvent) event;
        return true;
    }

    private boolean stepEvent(Event event) {
        StepEvent se = (StepEvent) event;
        return true;
    }

    private boolean classPrepareEvent(Event event) {
        ClassPrepareEvent cle = (ClassPrepareEvent) event;
        return false;
    }

    private boolean classUnloadEvent(Event event) {
        ClassUnloadEvent cue = (ClassUnloadEvent) event;
        return false;
    }

    private boolean exceptionEvent(Event event) {
        ExceptionEvent ee = (ExceptionEvent) event;
        return true;
    }

    private boolean threadDeathEvent(Event event) {
        ThreadDeathEvent tee = (ThreadDeathEvent) event;
        return false;
    }

    private boolean threadStartEvent(Event event) {
        ThreadStartEvent tse = (ThreadStartEvent) event;
        return false;
    }

    public boolean vmDeathEvent(Event event) {
        return false;
    }

    public boolean vmDisconnectEvent(Event event) {
        System.out.println("VMDisconnectEvent");
        return false;
    }

    private void jbInit() throws Exception {
    }

}