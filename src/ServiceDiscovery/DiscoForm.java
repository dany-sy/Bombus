/*
 * RegForm.java
 *
 * Created on 5 ���� 2005 �., 20:04
 */

package ServiceDiscovery;
import java.util.*;
import javax.microedition.lcdui.*;
import com.alsutton.jabber.*;
import com.alsutton.jabber.datablocks.*;
//import Client.*;


/**
 *
 * @author Evg_S
 */
public class DiscoForm implements CommandListener{
    
    private Display display;
    private Displayable parentView;
    
    private Vector fields;
    private String xmlns;
    private String service;
    
    private Form form;
    
    private boolean xData;
    
    private Command cmdOk=new Command("Send", Command.OK, 1);
    private Command cmdCancel=new Command("Cancel", Command.BACK, 99);
    
    private String id;
    
    //Roster roster=StaticData.getInstance().roster;
    JabberStream stream;
    
    /** Creates a new instance of RegForm */
    public DiscoForm(Display display, JabberDataBlock regform, JabberStream stream, String resultId) {
        service=regform.getAttribute("from");
        JabberDataBlock query=regform.getChildBlock("query");
        xmlns=query.getAttribute("xmlns");
        JabberDataBlock x=query.getChildBlock("x");
        this.id=resultId;
        // todo: ���������� ������ query
        fields=new Vector();
        Form form=new Form(service);

        // for instructions
        fields.addElement(null);
        form.append("-");
        
        Vector vFields=(xData=(x!=null))? x.getChildBlocks() : query.getChildBlocks();
        
        for (Enumeration e=vFields.elements(); e.hasMoreElements(); ){
            FormField field=new FormField((JabberDataBlock)e.nextElement());
            if (field.instructions) {
                fields.setElementAt(field, 0);
                form.set(0, field.formItem);
            } else {
                fields.addElement(field);
                if (!field.hidden) form.append(field.formItem);
            }
        }
        
       
        form.setCommandListener(this);
        form.addCommand(cmdOk);
        form.addCommand(cmdCancel);
        
        this.display=display;
        this.parentView=display.getCurrent();
        this.stream=stream;
        display.setCurrent(form);
    }
    
    private void sendForm(String id){
        JabberDataBlock req=new Iq(null, null);
        req.setTypeAttribute("set");
        req.setAttribute("to",service);
        req.setAttribute("id",id);
        JabberDataBlock qry=new JabberDataBlock("query",null,null);
        qry.setNameSpace(xmlns);
        req.addChild(qry);
        
        if (xData) {
            JabberDataBlock x=new JabberDataBlock("x", null,  null);
            x.setNameSpace("jabber:x:data");
            x.setAttribute("type", "submit");
            qry.addChild(x);
            qry=x;
        }
        
        for (Enumeration e=fields.elements(); e.hasMoreElements(); ) {
            FormField f=(FormField) e.nextElement();
            if (f==null) continue;
            JabberDataBlock ch=f.getJabberDataBlock();
            if (ch!=null) qry.addChild(ch);
        }
        //System.out.println(req.toString());
        System.out.println(req.toString());
        stream.send(req);
    }

    
    public void commandAction(Command c, Displayable d){
        if (c==cmdCancel) destroyView();
        if (c==cmdOk) { 
            sendForm(id);
            destroyView();
        }
    }

    public void destroyView(){
        display.setCurrent(parentView);
    }
}