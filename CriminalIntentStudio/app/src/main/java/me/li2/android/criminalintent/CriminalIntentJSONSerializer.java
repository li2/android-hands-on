package me.li2.android.criminalintent;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class CriminalIntentJSONSerializer {
    private static final String TAG = "CriminalIntentJSONSerializer";
    
    private Context mContext;
    private String mFilename;
    
    public CriminalIntentJSONSerializer(Context context, String filename) {
        mContext = context;
        mFilename = filename;
    }

    // Loading Crimes from the filesystem
    public ArrayList<Crime> loadCrimes() throws IOException, JSONException {
        ArrayList<Crime> crimes = new ArrayList<Crime>();
        BufferedReader reader = null;

        // Open and read the file into a StringBuffer， I/O流的典型使用方式。
        InputStream in;
        try {
            // Open a private file associated with this Context's application package for reading.
            // FileInputStream派生自InputStream类型，用于从文件中读取信息。
            in = mContext.openFileInput(mFilename);
            
            // Reader类主要是为了国际化，提供兼容Unicode与面向字符的I/O功能，而老的I/O流仅支持8位字节流。
            // 为了把“字节层次”中的类和“字符层次”中的类结合起来使用，需要用到“适配器”类：InputStreamReader()可以把InputStream转换为Reader
            // 为了提高速度，对文件进行缓冲，将产生的文件引用传给BufferedReader构造器。
            reader = new BufferedReader(new InputStreamReader(in));
            
            // 创建一个StringBuilder，用以构造最终的String，高效！
            StringBuilder jsonString = new StringBuilder();
            String line = null;
            
            // 当readLine()返回null时，就达到了文件末尾。
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            
            // Parse the JSON using JSONTokener
            // 把json格式文本转换成相应的对象，如JSONObject,JSONArray,String,boolean,int,long,double or NULL.
            JSONArray array = (JSONArray) new JSONTokener(jsonString.toString()).nextValue();
            
            // Build the array of crimes from JSONObjects
            for (int i = 0; i < array.length(); i++) {
                crimes.add(new Crime(array.getJSONObject(i)));
            }
        } catch (FileNotFoundException e) {
            // Ignore this one, it happens when starting fresh.
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        
        return crimes;
    }
    
    public void saveCrimes(List<Crime> crimes) throws JSONException, IOException {
        // Build an array in JSON
        JSONArray array = new JSONArray();
        for (Crime c : crimes) {
            array.put(c.toJSON());
        }
        
        // Write the file to disk
        Writer writer = null;
        try {
            // 使用标准的Java接口类来写入数据，OutputStreamWriter会解决JavaString与最终写入文件的原始字节流之间的转换。
            // 首先调用openFileOutput()方法获得OutputStream对象，
            OutputStream out = mContext.openFileOutput(mFilename, Context.MODE_PRIVATE);
            // 然后用其创建一个新的OutputStreamWriter对象,
            writer = new OutputStreamWriter(out);
            // 最后调用OutputStreamWriter的写入方法写入数据。
            writer.write(array.toString());
        } catch (FileNotFoundException e) {
        } finally {
            // 落下finally导致数据并没有写入JSON文件，
            // 当从JSON文件中load数据时，执行JSONTokener()遇到错误： org.json.JSONException: End of input at character 0.
            if (writer != null) {
                Log.e(TAG, mFilename + "not found!");
                writer.close();
            }
        }
    }
}
