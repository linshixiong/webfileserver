package com.linsx.webserver;
import java.lang.reflect.InvocationTargetException; 
import java.lang.reflect.Method; 
import android.content.Context;
import android.os.storage.StorageManager; 
 
public class StorageListHelper { 
    private Context mContext; 
    private StorageManager mStorageManager; 
    private Method mMethodGetPaths; 
    private Method mMethodGetState; 
     

	public StorageListHelper(Context context) { 
    	mContext = context; 
        if (mContext != null) { 
            mStorageManager = (StorageManager)mContext 
                    .getSystemService(Context.STORAGE_SERVICE); 
            try { 
                mMethodGetPaths = mStorageManager.getClass() 
                        .getMethod("getVolumePaths"); 
                mMethodGetState = mStorageManager.getClass() 
                        .getMethod("getVolumeState",String.class);
            } catch (NoSuchMethodException e) { 
                e.printStackTrace(); 
            } 
        } 
    } 
     
    public String[] getVolumePaths() { 
        String[] paths = null; 
        try { 
            paths = (String[]) mMethodGetPaths.invoke(mStorageManager); 
        } catch (IllegalArgumentException e) { 
            e.printStackTrace(); 
        } catch (IllegalAccessException e) { 
            e.printStackTrace(); 
        } catch (InvocationTargetException e) { 
            e.printStackTrace(); 
        } 
        return paths; 
    } 
    
    public String getVolumeState(String mountPoint){
    	 String state = null; 
         try { 
        	 state = (String) mMethodGetState.invoke(mStorageManager,mountPoint); 
         } catch (IllegalArgumentException e) { 
             e.printStackTrace(); 
         } catch (IllegalAccessException e) { 
             e.printStackTrace(); 
         } catch (InvocationTargetException e) { 
             e.printStackTrace(); 
         } 
         return state; 
    }
    
    
} 