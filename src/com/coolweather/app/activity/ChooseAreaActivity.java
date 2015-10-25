package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;
import com.coolweather.app.model.City;
import com.coolweather.app.model.CoolWeatherDB;
import com.coolweather.app.model.Country;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

public class ChooseAreaActivity extends Activity  {
	
	public static final int LEVEL_PROVINCE = 0;
	
	public static final int LEVEL_CITY = 1;
	
	public static final int LEVEL_COUNTRY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	/**
	 * ʡ�б�
	 */
	private List<Province> provinceList;
	/**
	 * ���б�
	 */
	private List<City> cityList;
	/**
	 * ���б�
	 */
	private List<Country> countryList;
	
	/**
	 * ѡ�е�ʡ��
	 */
	private Province selectedProvince;
	/**
	 * ѡ�еĳ���
	 */
	private City selectedCity;
	/**
	 * ��ǰѡ�еļ���
	 */
	private int currentLevel;

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("city_selected", false)) {
        	Intent intent = new Intent(this,WeatherActivity.class);
        	startActivity(intent);
        	finish();
        	return ;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String> (this,android.R.layout.simple_list_item_1,dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0,View view, int index, long arg3) {
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				}else if (currentLevel == LEVEL_COUNTRY) {
					String countryCode = countryList.get(index).getCountryCode();
					Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("country_code",countryCode);
					startActivity(intent);
					finish();
				}
			}
		});
		queryProvinces();//����ʡ������
		Log.d("Provinces","load province data over");
		
	}
    
    /**
     * ��ѯȫ�����е�ʡ�����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
     */
    private void queryProvinces() {
    	provinceList = coolWeatherDB.loadProvinces();
    	if(provinceList.size() > 0) {
    		dataList.clear();
    		for (Province province : provinceList) {
    			dataList.add(province.getProvinceName());
    		}
    		adapter.notifyDataSetChanged();
    		listView.setSelection(0);
    		titleText.setText("�й�");
    		currentLevel = LEVEL_PROVINCE;
    	} else {
    		Log.d("Provinces","we need to load from server");
    		queryFromServer(null,"province");
    	}
    }
    
    /**
     * ��ѯѡ��ʡ�����е��У����ȴ����ݿ��ѯ�����û���ٵ��������ϲ�ѯ
     */
    private void queryCities() {
    	cityList = coolWeatherDB.loadCities(selectedProvince.getId());
    	Log.d("cityList = ","citylist.size" + cityList.size() );
    	if(cityList.size() > 0) {
    		dataList.clear();
    		for (City city : cityList) {
    			dataList.add(city.getCityName());
    		}
    		adapter.notifyDataSetChanged();
    		listView.setSelection(0);
    		titleText.setText(selectedProvince.getProvinceName());
    		currentLevel = LEVEL_CITY;
    	} else {
    		Log.d("ProvinceCode","provinceCode is " + selectedProvince.getProvinceCode());
    		queryFromServer(selectedProvince.getProvinceCode(),"city");
    	}
    }
    
    /**
     * ��ѯѡ���������е��أ����ȴ����ݿ��ѯ�����û���ٵ��������ϲ�ѯ
     */
    private void queryCounties() {
    	countryList = coolWeatherDB.loadCounties(selectedCity.getId());
    	if (countryList.size() > 0) {
    		dataList.clear();
    		for (Country country : countryList) {
    			dataList.add(country.getCountryName());
    		}
    		adapter.notifyDataSetChanged();
    		listView.setSelection(0);
    		titleText.setText(selectedCity.getCityName());
    		currentLevel = LEVEL_COUNTRY;
    	} else {
    		queryFromServer(selectedCity.getCityCode(),"country");
    	}
    }
    
    /**
     * ���ݴ���Ĵ��ź����ʹӷ������ϲ�ѯʡ��������.
     */
    private void queryFromServer(final String code, final String type) {
    	String address;
    	if (!TextUtils.isEmpty(code)) {
    		address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
    	} else {
    		address = "http://www.weather.com.cn/data/list3/city.xml";
    	}
    	showProgressDialog();
    	Log.d("provinces","in queryFromServer");
    	HttpUtil.sendHttpRequest(address, new HttpCallbackListener () {
    		@Override
    		public void onFinish(String response) {
    			boolean result = false;
    			if ("province".equals(type)) {
    				result = Utility.handleProvincesResponse(coolWeatherDB, response);
    			} else if ("city".equals(type)) {
    				result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
    			} else if ("country".equals(type)) {
    				result = Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
    			}
    			if (result) {
    				Log.d("onFinish","In the onfinish");
    				//ͨ��runOnUiThread()�����ص����̴߳����߼�
    				runOnUiThread(new Runnable() {
    					@Override
    					public void run() {
    						closeProgressDialog();
    						if("province".equals(type)) {
    							queryProvinces();
    						} else if ("city".equals(type)) {
    							queryCities();
    						} else if ("country".equals(type)) {
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
    					Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_SHORT).show();
    				}
    			});
    		}
    	});
    }
    
    /**
     * ��ʾ���ȶԻ���
     */
    private void showProgressDialog() {
    	if (progressDialog == null) {
    		progressDialog = new ProgressDialog(this);
    		progressDialog.setMessage("���ڼ���...");
    		progressDialog.setCanceledOnTouchOutside(false);
    	}
    	progressDialog.show();
    }
    
    /**
     * �رս��ȶԻ���
     */
    private void closeProgressDialog() {
    	if (progressDialog != null) {
    		progressDialog.dismiss();
    	}
    }
    
    /**
     * ����back���������ݵ�ǰ�ļ������жϣ���ʱӦ�÷������б�ʡ�б�����ֱ���˳���
     */
    @Override
    public void onBackPressed() {
    	if (currentLevel == LEVEL_COUNTRY) {
    		queryCities();
    	} else if (currentLevel == LEVEL_CITY) {
    		queryProvinces();
    	} else {
    		finish();
    	}
    }
}
