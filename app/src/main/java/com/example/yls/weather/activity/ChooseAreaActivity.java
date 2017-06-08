package com.example.yls.weather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yls.weather.R;

import java.util.ArrayList;
import java.util.List;

import db.WeatherDB;
import modle.City;
import modle.County;
import modle.Province;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

/**
 * Created by yls on 2017/6/7.
 */

public class ChooseAreaActivity extends Activity{

    private boolean isFromWeatherActivity;
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;

    private ProgressDialog mProgressDialog;
    private TextView mTextView;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private WeatherDB mWeatherDB;
    private List<String> dataList=new ArrayList<String>();
    private List<Province> mProvinceList;
    private List<City> mCityList;
    private List<County> mCountyList;
    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;

    protected void onCreate(Bundle savedInstanceSate){
        super.onCreate(savedInstanceSate);
        isFromWeatherActivity=getIntent().getBooleanExtra("from_weather_activity",false);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("city_selected",false)&&!isFromWeatherActivity) {
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        mListView= (ListView) findViewById(R.id.list_view);
        mTextView= (TextView) findViewById(R.id.title_text);
        mAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        mListView.setAdapter(mAdapter);
        mWeatherDB=WeatherDB.getInstance(this);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=mProvinceList.get(index);
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=mCityList.get(index);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    String countyCode=mCountyList.get(index).getCountyCode();
                    Intent intent=new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }


            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        mProvinceList=mWeatherDB.loadProvince();
        if(mProvinceList.size()>0){
            dataList.clear();
            for(Province province:mProvinceList){
                dataList.add(province.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mTextView .setText("China");
            currentLevel=LEVEL_PROVINCE;
        }else{
            queryFromServer(null,"province");
        }
    }



    private void queryCities() {
        mCityList=mWeatherDB.loadCites(selectedProvince.getId());
        if(mCityList.size()>0){
            dataList.clear();
            for(City city:mCityList){
                dataList.add(city.getCityName());
            }

            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mTextView.setText(selectedProvince.getProvinceName());
            currentLevel=LEVEL_CITY;
        }else{
            queryFromServer(selectedProvince.getProvinceCode(),"city");

        }
    }

    private  void queryCounties(){
        mCountyList=mWeatherDB.loadCounty(selectedCity.getId());
        if(mCountyList.size()>0){
            dataList.clear();
            for(County county:mCountyList){
                dataList.add(county.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mTextView.setText(selectedCity.getCityName());
            currentLevel=LEVEL_COUNTY;
        }else{
            queryFromServer(selectedCity.getCityCode(),"county");
        }

    }
    private void queryFromServer(final String code,final String type) {
         String address;
        if(!TextUtils.isEmpty(code)){
            address="http://www.weather.com.cn/data/list3/city" + code + ".xml";
        }else{
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address,new HttpCallbackListener(){

            @Override
            public void onFinish(String response) {
                boolean result=false;
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(mWeatherDB,response);
                }else if("city".equals(type)){
                    result=Utility.handleCitiesResponse(mWeatherDB,response,selectedProvince.getId());
                }else if("county".equals(type)){
                    result=Utility.handleCountiesResponse(mWeatherDB,response,selectedCity.getId());
                }
                if(result){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }
                           else if("city".equals(type)){
                                queryCities();
                            }
                            else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,
                                "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

    }

    private void showProgressDialog() {
        if(mProgressDialog==null){
            mProgressDialog=new ProgressDialog(this);
            mProgressDialog.setMessage("loading");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }


    private void closeProgressDialog() {
        if(mProgressDialog!=null){
            mProgressDialog.dismiss();
        }
    }
    public void onBackPressed(){
        if(currentLevel==LEVEL_COUNTY){
            queryCities();
        }
        else if(currentLevel==LEVEL_CITY){
            queryProvinces();
        }else{
            if(isFromWeatherActivity){
                Intent intent=new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }

}
