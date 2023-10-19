//
// Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
//

package com.cyberesicg.oscal_cprt.logic;

//

import com.cyberesicg.oscal_cprt.gui.MainGuiFrame;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

// @author jimlh
//
public class AppUtils {
    static public String gOutFolder = "."+File.separator;
    static public MainGuiFrame gMainFrame = null;
    
    //Returns OSCAL date/time string
    //If param is null uses current date/time
    static String getDateTime(Date dateToConvert){
        Date inDate;
        if(dateToConvert == null){
            inDate = Calendar.getInstance().getTime();
        } else inDate = dateToConvert;
        //2022-08-31T00:00:00-04:00
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssXXX"); 
        String dtStr = df.format(inDate);
        
        return dtStr;
    }
    
    static String getFNameDateTime(Date dateToConvert){
        Date inDate;
        if(dateToConvert == null){
            inDate = Calendar.getInstance().getTime();
        } else inDate = dateToConvert;
        //220831000000
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss"); 
        String dtStr = df.format(inDate);
        
        return dtStr;
    }
    
    public static boolean isNumeric(String str) { 
        try {  
            Double.parseDouble(str);  
            return true;
        } catch(NumberFormatException e){  
            return false;  
        }  
    }
}
