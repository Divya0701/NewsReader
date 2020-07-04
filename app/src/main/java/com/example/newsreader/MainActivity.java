package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    ArrayList<String> links =new ArrayList<>();
    ListView listView;

     SQLiteDatabase DB ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        DB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,content VARCHAR )");



        DownloadTask task =new DownloadTask();
        try {
           // task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }

       listView=findViewById(R.id.listview);
       arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
       listView.setAdapter(arrayAdapter);

       listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
               Intent intent =new Intent(getApplicationContext(),ArticleActivity.class);
               intent.putExtra("content",links.get(i));
               startActivity(intent);
           }
       });
        updateListview();
    }
    public void updateListview(){
        Cursor c= DB.rawQuery("SELECT * FROM articles",null);
        int contentIndex =c.getColumnIndex("content");
        int titleIndex =c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            links.clear();
            do{
                titles.add(c.getString(titleIndex));
                links.add(c.getString(contentIndex));
            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String , Void, String>{

        @Override
        protected String doInBackground(String... urls) {
            URL url;
            HttpURLConnection urlConnection =null;
            String  Result="";
            try{
                url= new URL(urls[0]);
                urlConnection = (HttpURLConnection)url.openConnection();
                InputStream in =urlConnection.getInputStream();
                InputStreamReader reader =new InputStreamReader(in);
                int data = reader.read();
                while(data!=-1){
                    char current =(char)data;
                    Result += current;
                    data = reader.read();
                }
                JSONArray jsonArray =new JSONArray(Result);
                int numberOfItems =20;
                if(jsonArray.length()<20){
                    numberOfItems = jsonArray.length();
                }
                DB.execSQL("DELETE FROM articles");
                for(int i=0;i<numberOfItems;i++){
                    String ArticleID = jsonArray.getString(i);
                    String Article ="";
                    url= new URL("https://hacker-news.firebaseio.com/v0/item/"+ArticleID+".json?print=pretty");
                    urlConnection = (HttpURLConnection)url.openConnection();
                     in =urlConnection.getInputStream();
                     reader =new InputStreamReader(in);
                     data = reader.read();
                    while(data!=-1){
                        char current =(char)data;
                        Article += current;
                        data = reader.read();
                    }
                  JSONObject jsonObject =new JSONObject(Article);
                    if(!jsonObject.isNull("title")&&!jsonObject.isNull("url")){
                        String articleTitle =jsonObject.getString("title");
                        String articleURL =jsonObject.getString("url");

                       url =new URL(articleURL);
                       String html = "";
                       urlConnection = (HttpURLConnection)url.openConnection();
                       in =urlConnection.getInputStream();
                       reader =new InputStreamReader(in);
                       data =reader.read();
                       while(data!=-1){
                           char current =(char)data;
                           html +=current;
                           data=reader.read();
                       }
                       Log.i("DATA",html);
                       String sql ="INSERT INTO articles(articleId, title ,content) VALUES (?,?,?)";
                        SQLiteStatement statement =DB.compileStatement(sql);
                        statement.bindString(1,ArticleID);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,html);
                        statement.execute();


                    }
                }


                return Result;
            }
            catch (Exception e){
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListview();
        }
    }
}
