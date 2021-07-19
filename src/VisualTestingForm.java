import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VisualTestingForm  extends JFrame {
    private JButton fileButton;
    private JButton runButton;
    private JList list1;
    private JPanel VisualTestingForm;
    private JTextField filePath;
    private JScrollPane board2;
    private JTextArea textArea;
    private File selectedFile;
    private File[] files;
    private Constructor[] cons;
    private Method[] methods;
    private Object[] obj;
    private HashMap<String, Integer> hm;
    private MyThread mt;
    private MouseEvent me;

    public VisualTestingForm(String title){
        super(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(VisualTestingForm);
        this.pack();

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File_actionListener(e);
            }
        });
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Run_actionListener(e);
            }
        });
        list1.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ClassSkeleton();
            }
        });
    }
    //file path: D:\Huy T Tran\Third Year\Spring Semester\SWENG 431\flood_filling
    void File_actionListener(ActionEvent e){
        JFileChooser jfc= new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setMultiSelectionEnabled(true);
        int openCode =jfc.showOpenDialog(null);
        if(openCode==JFileChooser.APPROVE_OPTION){
            filePath.setText(jfc.getSelectedFile().getAbsolutePath());
            files=jfc.getSelectedFile().listFiles();
            selectedFile= jfc.getSelectedFile();
        }
        String[] fileName= new String[files.length];
        for(int i=0; i<files.length; i++){
            if(files[i].getName().endsWith(".class")) {
                fileName[i]= files[i].getName();
            }
        }
        list1.setListData(fileName);
    }

    void ClassSkeleton(){
        //clear the text area
        textArea.setText("");
        //show tracking numbers
        JViewport jvp = new JViewport();
        JTextArea jta = new JTextArea();
        jta.setEnabled(false);
        board2.setRowHeader(jvp);
        Object selected = list1.getSelectedValue();
        File file= null;
        for(File i: files){
            if(i.getName().equals(selected.toString())){
                file=i; break;
            }
        }
        try{
            String fileName = file.getName().split("\\.")[0]; // removing the ".class" suffix
            String parentFileName = file.getParentFile().getName();
            URL url = file.getParentFile().getParentFile().toURI().toURL();
            URL[] urla = {url};
            URLClassLoader ucl = new URLClassLoader(urla);
            Class c = Class.forName(parentFileName + "." + fileName, true, ucl);
            cons = c.getConstructors();
            Field[] fields=c.getDeclaredFields();
            textArea.append(c.toGenericString()+"{\n");
            jta.append("\n");
            int numberOfFields= fields.length, i=0;

            while(i++<numberOfFields) jta.append("\n");

            for (Field f: fields) textArea.append("\t"+f.toGenericString().replaceAll(c.getName()+".", "")+";\n");
            if(hm==null){
                hm= new HashMap<>();
                collectNumberOfMethodsAndConstructors();
            }else if(mt!=null){
                hm= mt.returnHashMap();
            }else{
                collectNumberOfMethodsAndConstructors();
            }
            for(Constructor constructor: cons) {
                jta.append(hm.get(constructor.getName())+"\n");
                textArea.append("\t"+formatConstructor(constructor,parentFileName)+"\n");
            }
            methods= c.getDeclaredMethods();
            for(Method method: methods) {
                jta.append(hm.get(method.getName())+"\n");
                textArea.append("\t"+formatMethod(method)+"\n");
            }
            textArea.append("}");
            jvp.add(jta);
        } catch(MalformedURLException | ClassNotFoundException mal){
            mal.printStackTrace();
        }
    }

    private void collectNumberOfMethodsAndConstructors(){
        for(File i: files){
            if (i.getName().endsWith(".class")){
                ArrayList<Integer> num= new ArrayList<>();
                try {
                    String fileName = i.getName().split("\\.")[0]; // removing the ".class" suffix
                    String parentFileName = i.getParentFile().getName();
                    URL url = i.getParentFile().getParentFile().toURI().toURL();
                    URL[] urla = {url};
                    URLClassLoader ucl = new URLClassLoader(urla);
                    Class c = Class.forName(parentFileName + "." + fileName, true, ucl);
                    cons = c.getConstructors();
                    for(Constructor constructor: cons){
                        hm.put(constructor.getName(), 0);
                    }
                    methods= c.getDeclaredMethods();
                    for(Method method: methods) {
                        hm.put(method.getName(), 0);
                    }
                } catch(MalformedURLException | ClassNotFoundException mal){
                    mal.printStackTrace();
                }
            }
        }
    }
    void Run_actionListener(ActionEvent e){
        VirtualMachine vm;
        LaunchingConnector lc = Bootstrap.virtualMachineManager().defaultConnector();
        Map map = lc.defaultArguments();
        Connector.Argument ca = (Connector.Argument) map.get("main");
        int index =list1.getSelectedIndex();
        try{
            int i = files[index].getName().indexOf(".class");
            String className = selectedFile.getName()+"."+files[index].getName().substring(0,i);
            ca.setValue("-cp \"" +selectedFile.getParentFile()+"\""+className);
            vm=lc.launch(map);
            Process p =vm.process();
            vm.setDebugTraceMode(VirtualMachine.TRACE_NONE);
            displayOutput(p.getInputStream());
            mt = new MyThread(vm, false, selectedFile.getName(), hm, this);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private void displayOutput(InputStream in){
        Thread t = new Thread("output viewer");
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }
    public static void main(String[] args){
        JFrame frame = new VisualTestingForm("Visual Testing Form");
        frame.setSize(800,800);
        frame.setVisible(true);
    }

    public String returnModifier(int modifierCode){
        String str="";
        if(modifierCode==1) {
            str+="public ";
        }
        else if(modifierCode==2) {
            str+="private ";
        }
        else if(modifierCode==4) {
            str+="protected ";
        }
        else if(modifierCode==8) {
            str+="static ";
        }
        else if(modifierCode==16) {
            str+="final ";
        }
        else if(modifierCode==512) {
            str+="interface ";
        }
        else if(modifierCode==1024) {
            str+="abstract ";
        }
        return str;
    }
    public String toType(java.lang.reflect.Type[] types){
        int i=0, d=0, b=0, s=0, k=0; //i for integer, d for double, b for boolean, s for string, k for generic type
        String finalFormat="";
        for(int index=0; index<types.length; index++){
            if(types[index].getTypeName().equals("int")){
                finalFormat=finalFormat+"int i"+ i++;
            }else if(types[index].getTypeName().equals("double")){
                finalFormat=finalFormat+"double d"+ d++;
            }else if(types[index].getTypeName().equals("boolean")){
                finalFormat=finalFormat+"boolean b"+ b++;
            }else if(types[index].getTypeName().equals("String")){
                finalFormat=finalFormat+"String s"+ s++;
            }else {
                finalFormat=finalFormat+types[index].getTypeName().replaceAll("java.lang.", "")+" k"+ k++;
            }
            if(index<types.length-1) finalFormat+=", ";
        }
        return finalFormat;
    }
    public String formatConstructor(Constructor constructor, String fileName){
        String cons="";
        cons+=returnModifier(constructor.getModifiers());
        cons+= constructor.getName().substring(fileName.length()+1);
        cons+="(";
        java.lang.reflect.Type[] types=constructor.getParameterTypes();
        cons+=(toType(types));
        cons+=(");");
        return cons;
    }
    public String formatMethod(Method method){
        String m="";
        m+=Modifier.toString(method.getModifiers());
        java.lang.reflect.Type type=method.getGenericReturnType();
        java.lang.reflect.Type[] types=method.getGenericParameterTypes();
        m+=" "+type.getTypeName().replaceAll("java.lang.","")+" "+method.getName()+"("+toType(types)+");";
        return m;
    }
}
