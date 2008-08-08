/*
 * vCard.java
 *
 * Created on 24.09.2005, 1:24
 *
 * Copyright (c) 2005-2007, Eugene Stahov (evgs), http://bombus-im.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * You can also redistribute and/or modify this program under the
 * terms of the Psi License, specified in the accompanied COPYING
 * file, as published by the Psi Project; either dated January 1st,
 * 2005, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package vcard;
import Client.StaticData;
import com.alsutton.jabber.JabberDataBlock;
import com.alsutton.jabber.datablocks.Iq;
import java.util.*;
import java.io.*;
import javax.microedition.lcdui.Image;
import util.StringLoader;
import util.strconv;

/**
 *
 * @author EvgS
 */
public class VCard {

    public final static int NICK_INDEX=1;
    public final static int FN_INDEX=0;
    
    public static Vector vCardFields;
    public static Vector vCardFields2;
    public static Vector vCardLabels;
    
    private Vector vCardData;
    private String jid;
    private String id;
    
    byte photo[];
    
    private boolean empty=true;
    
    /** Creates a new instance of vCard */
    public VCard() {
        if (vCardFields==null) fieldsLoader();
    }
    
    public VCard(JabberDataBlock data) {
        this();
        jid=data.getAttribute("from");
        id=data.getAttribute("id");
        int itemsCount=getCount();
        vCardData=new Vector(itemsCount);
        vCardData.setSize(itemsCount);
        
        if (data==null) return; 
        if (data.getTypeAttribute().equals("error")) return;
        JabberDataBlock vcard=data.findNamespace("vCard", "vcard-temp");
        if (vcard==null) return; //"No vCard available" 

        empty=false;
        
        for (int i=0; i<itemsCount; i++){
            try {
                String f1=(String)VCard.vCardFields.elementAt(i);
                String f2=(String)VCard.vCardFields2.elementAt(i);
                
                JabberDataBlock d2=
                        (f2==null) ? vcard : vcard.getChildBlock(f2);
                
                String field=d2.getChildBlockText(f1);
                
                if (field.length()>0) setVCardData(i, field);
            } catch (Exception e) {/**/}
        }
        
       try {
           JabberDataBlock photoXML=vcard.getChildBlock("PHOTO").getChildBlock("BINVAL");
           photo=(byte[])photoXML.getChildBlocks().lastElement();
       } catch (Exception e) {};
    }

    public JabberDataBlock constructVCard(){
        JabberDataBlock vcardIq=new Iq(null, Iq.TYPE_SET, "vcard-set");
        JabberDataBlock vcardTemp=vcardIq.addChildNs("vCard", "vcard-temp");
        
        int itemsCount=getCount();
        
        for (int i=0; i<itemsCount; i++){
            String field=getVCardData(i);
            if (field==null) continue;
            
            String f1=(String)VCard.vCardFields.elementAt(i);
            String f2=(String)VCard.vCardFields2.elementAt(i);
            
            JabberDataBlock subLevel=vcardTemp;
            if (f2!=null) {
                subLevel=vcardTemp.getChildBlock(f2);
                if (subLevel==null) subLevel=vcardTemp.addChild(f2, null);
            }
            subLevel.addChild(f1, field);
            
        }
        if (photo!=null) {
            String mime=getPhotoMIMEType();
            if (mime!=null) {
                JabberDataBlock ph=vcardTemp.addChild("PHOTO", null);
                ph.addChild("BINVAL", strconv.toBase64(photo, -1));
                ph.addChild("TYPE", mime);
            }
        }
        //System.out.println(vcard.toString());
        return vcardIq;
    }
    
    public byte[] getPhoto() { return photo; }
    
    public String getPhotoMIMEType() {
        try {
            if (photo.length==1) return null;
            
            if (photo[0]==(byte)0xff &&
                photo[1]==(byte)0xd8 &&
                ((photo[6]==(byte)'J' &&
                  photo[7]==(byte)'F' &&
                  photo[8]==(byte)'I' &&
                  photo[9]==(byte)'F') 
                  ||
                 (photo[6]==(byte)'E' &&
                  photo[7]==(byte)'x' &&
                  photo[8]==(byte)'i' &&
                  photo[9]==(byte)'f')
                 )
                )
                return "image/jpeg";
            
            if (photo[0]==(byte)0x89 &&
                photo[1]==(byte)'P' &&
                photo[2]==(byte)'N' &&
                photo[3]==(byte)'G')
                return "image/png";
            
            if (photo[0]==(byte)'G' &&
                photo[1]==(byte)'I' &&
                photo[2]==(byte)'F')
                return "image/gif";
            
            if (photo[0]==(byte)'B' &&
                photo[1]==(byte)'M')
                return "image/x-ms-bmp";
        } catch (Exception e) {}
        return null;
    }
    
    public void setPhoto(byte[] photo) {
        this.photo=photo;
    }
    
    public String getNickName() { 
        String name=getVCardData(NICK_INDEX);
        if (name!=null) return name;
        return getVCardData(FN_INDEX);
    }
    
    public static JabberDataBlock getQueryVCard(String to, String id ) 
    {
        JabberDataBlock req=new Iq(to, Iq.TYPE_GET, id);
        req.addChildNs("vCard", "vcard-temp" );

        return req;
    }
    
    public static void request(String jid, String id) {
        StaticData.getInstance().roster.setQuerySign(true); 
        StaticData.getInstance().roster.theStream.send(getQueryVCard(jid, "getvc"+id));
    }
    
    private void fieldsLoader(){
        String vcardFile="/lang/"+Client.Config.getInstance().lang+".vcard.txt";
        Vector table[]=new StringLoader().stringLoader(vcardFile, 3);
        
        if (table==null) table=new StringLoader().stringLoader("/lang/en.vcard.txt", 3);
        
	vCardFields=table[1];
        vCardFields2=table[0];
        vCardLabels=table[2];
        
    }
    public String getVCardData(int index) {
        return (String) vCardData.elementAt(index);
    }

    public void setVCardData(int index, String data) {
        vCardData.setElementAt(data, index);
    }
    
    public int getCount(){ return vCardFields.size(); }

    public String getJid() { return jid; }

    public String getId() {
        return id;
    }

    public boolean isEmpty() {
        return empty;
    }


}
