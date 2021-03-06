/*
 * Copyright (C) 2013  Spunk Media Pvt Ltd (www.qubecell.com)
 */

package com.qubecell.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qubecell.beans.EventChargeRespBean;
import com.qubecell.beans.ResponseBaseBean;
import com.qubecell.beans.SendOTPRespBean;
import com.qubecell.constants.ApplicationActivities;
import com.qubecell.constants.ConstantStrings;
import com.qubecell.constants.IntentConstant;
import com.qubecell.constants.MobileOperators;
import com.qubecell.constants.MsisdnServerRespCode;
import com.qubecell.constants.NetworkResponse;
import com.qubecell.constants.NetworkResponseCode;
import com.qubecell.constants.PaymentResult;
import com.qubecell.constants.ServerCommand;
import com.qubecell.constants.WidgetsTagName;
import com.qubecell.elogger.ELogger;
import com.qubecell.network.AsyncClient;
import com.qubecell.network.NetworkController;
import com.qubecell.smsmgr.QubecellSMSManager;
import com.qubecell.utility.CommonUtility;
import com.qubecell.xmlparser.XMLParser;

/**
 * Copyright (C) 2013  Spunk Media Pvt Ltd (www.qubecell.com)
 * The ValidateOTPActivity class is used to display screen for submitting OTP validation code and sending to 
 * server for validation.
 * @author Eninov
 *
 */
public class ValidateOTPActivity extends BaseActivity
{
	private Button nextButton = null;
	private Button backButton = null;
	private EditText validateCode = null;
	private ELogger log = null;
	private String transactionId = null;
	private TextView resendLink = null;
	private String msisdn = null;
	private int resendCount = 0;
	private String logTag = "ValidateOTPActivity";
	private View validateOTPView = null;
	private String onSavedOtpCode = "otpCodeKey";
	private String onSavedResendOtpCount = "otpCodeCountKey";
	private int MAX_COUNT = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		validateOTPView = CommonUtility.getValidateOTPlayoutView(getApplicationContext());
		setContentView(validateOTPView);
		initLogger();
		log.debug("Inside validate otp activity");
		getTransIntentData(getIntent());
		String savedOtpCode = null;
		if(savedInstanceState != null)
		{
			savedOtpCode = savedInstanceState.getString(onSavedOtpCode);
			resendCount = savedInstanceState.getInt(onSavedResendOtpCount);
		}
		initializeWidget(savedOtpCode);
		handleItemClickListener();
		hideKeyboardFromScreen();
		setCurrentActivity(ApplicationActivities.VALIDATE_OTP_ACTIVITY);
	}

	/**
	 * This method is used to save data on orientation change.
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle saveData) {
		super.onSaveInstanceState(saveData);
		String otpCode = ((EditText)validateOTPView.findViewWithTag(WidgetsTagName.VALIDATE_EDITTEXT)).getText().toString();		
		if(!TextUtils.isEmpty(otpCode))
		{
			saveData.putString(onSavedOtpCode, otpCode);
		}
		int resendOtpCount = resendCount;
		saveData.putInt(onSavedResendOtpCount, resendOtpCount);
		return;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if(getCurrentActivity().equalsIgnoreCase(ApplicationActivities.CLOSE_ACTIVITY))
		{
			this.finish();
		}
		return;
	}

	/**
	 * The method hideKeyboardFromScreen is used to hide the keyboard from screen.
	 * @param editText 
	 */
	private void hideKeyboardFromScreen() 
	{
		try
		{
			validateCode.setFocusable(true);
			validateCode.clearFocus();
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(validateCode.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			ValidateOTPActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		}
		catch(Exception e)
		{
			log.error("hideKeyBoardFromScreen() : Exception is :"+ e);
		}
		return;
	}


	/**
	 * This method is used to get intent data and set it in class variable.
	 */
	private void getTransIntentData(Intent intent) 
	{
		if(intent == null)
		{
			log.error("setIntentData() : Intent is found null");
			return;
		}
		transactionId = intent.getStringExtra(IntentConstant.TRANSACTION_ID); 
		msisdn = intent.getStringExtra(IntentConstant.MSISDN);
		operator = intent.getStringExtra(IntentConstant.OPERATOR_INFO);
		username = getUsername(); 
		password = getPassword();
		return;
	}

	/**
	 * This method is used to initialize the widgets
	 */
	private void initializeWidget(String savedOtpCode) 
	{
		nextButton = ((Button)validateOTPView.findViewWithTag(WidgetsTagName.VALIDATE_NEXT_BUTTONVIEW));
		nextButton.setBackgroundColor(getBackGroundColor());
		backButton = ((Button)validateOTPView.findViewWithTag(WidgetsTagName.VALIDATE_BACK_BUTTONVIEW));
		validateCode = ((EditText)validateOTPView.findViewWithTag(WidgetsTagName.VALIDATE_EDITTEXT));
		if(getBillingPartner() != null)
		{
			TextView poweredBy = ((TextView)validateOTPView.findViewWithTag(WidgetsTagName.VALIDATE_BOTTOM_TEXTVIEW));
			poweredBy.setText(getBillingPartner());
		}
		if(!TextUtils.isEmpty(savedOtpCode))
		{
			validateCode.setText(savedOtpCode);
		}
		resendLink = ((TextView)validateOTPView.findViewWithTag(WidgetsTagName.VALIDATE_SENDPTP_TEXTVIEW));
		if(getLogoImage() != null)
		{
			ImageView logoImageView = ((ImageView)validateOTPView.findViewWithTag(WidgetsTagName.VALIDATE_TOPHEADER_IMAGEVIEW));
			Drawable logoDrawable = new BitmapDrawable(getLogoImage());
			logoImageView.setBackgroundDrawable(logoDrawable);
		}
		LinearLayout mainFrame = ((LinearLayout)validateOTPView.findViewWithTag(WidgetsTagName.VALIDATE_MAIN_FRAME));
		mainFrame.setBackgroundColor(getThemeColor());
		return;
	}

	/**
	 * This method is used to handle the click listeners of button.
	 */
	private void handleItemClickListener() 
	{
		resendLink.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) 
			{
				if(resendCount == MAX_COUNT)
				{
					Intent intent = new Intent(ValidateOTPActivity.this, ResultActivity.class);
					intent.putExtra(IntentConstant.PAYMENT_RESULT, PaymentResult.FALIURE);
					intent.putExtra(IntentConstant.MESSAGE, ConstantStrings.UNABLE_TO_GENERATE_OTP);
					startActivity(intent);
					finish();
					return;
				}
				resendCount++;
				if(noDataConnection)
				{
					String txt = getProductId(MobileOperators.NODATA);
					if(txt != null )
					{
						String msgBody = null;
						msgBody = messageFormat + txt ;
						log.info("handleItemClickListener() : message body is : "+ msgBody);
						QubecellSMSManager smsObj = QubecellSMSManager.getInstance();
						smsObj.sendMessage(appContext, msgBody, smsShortCode, false);
					}
					else
					{
						log.error("ValidateOTP:: product id not found.");
						return;
					}
				}
				else
				{
					List<NameValuePair> requestParam = new ArrayList<NameValuePair>();
					requestId = String.valueOf(CommonUtility.getRandomNumberBetween());
					String chargeKey = getchargeKey();
					String md5Str = null;
					if(chargeKey != null)
					{
						md5Str = getMD5(chargeKey+requestId);
					}
					else
					{
						log.error("Authentication key not found.");
					}
					log.info("Resend request id is: " + requestId);
					if((operator != null) && (msisdn != null) && (operator.equalsIgnoreCase(MobileOperators.IDEA)))
					{
						setChargedAmount("0");
						requestParam.add(new BasicNameValuePair(ConstantStrings.USERNAME, username));
						requestParam.add(new BasicNameValuePair(ConstantStrings.PASSWORD, password));
						requestParam.add(new BasicNameValuePair(ConstantStrings.REQUESTID, requestId));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATION, "eventcharge"));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATOR, getProductId(operator))); 
						requestParam.add(new BasicNameValuePair(ConstantStrings.MESSAGE, "Event charge request"));
						requestParam.add(new BasicNameValuePair(ConstantStrings.KEY, md5Str));
						requestParam.add(new BasicNameValuePair(ConstantStrings.MSISDN, msisdn));
						requestParam.add(new BasicNameValuePair(ConstantStrings.RETURNURL, ""));
						requestParam.add(new BasicNameValuePair(ConstantStrings.LOG_PATH, ""));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATOR, operator));
						makeNetworkRequest(requestParam, ServerCommand.EVENTCHARGE_CMD);
					}
					else
					{
						requestParam.add(new BasicNameValuePair(ConstantStrings.USERNAME, username));
						requestParam.add(new BasicNameValuePair(ConstantStrings.PASSWORD, password));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATION, "sendotp"));
						requestParam.add(new BasicNameValuePair(ConstantStrings.REQUESTID, requestId));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATOR, operator));
						requestParam.add(new BasicNameValuePair(ConstantStrings.MESSAGE, ""));
						requestParam.add(new BasicNameValuePair(ConstantStrings.KEY, md5Str));
						requestParam.add(new BasicNameValuePair(ConstantStrings.MSISDN, msisdn));
						makeNetworkRequest(requestParam, ServerCommand.SENDOTP_CMD);
					}
				}
			}
		});

		// Back Button Click listener
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) 
			{
				if(noDataConnection)
				{
					finish();
				}
				else
				{
					Intent intent = new Intent(getApplicationContext(), SelectOperatorActivity.class);
					startActivity(intent);
					finish();
				}
			}
		});

		// Next button click listener
		nextButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) 
			{
				String validateCodeStr = validateCode.getText().toString();
				if(!TextUtils.isEmpty(validateCodeStr))
				{
					if(noDataConnection)
					{
						String proId  = getProductId(MobileOperators.NODATA);
						if(proId == null)
						{
							log.error(" product id not found.");
							proId = "null";
						}
						String msgBody = validateFormat.concat(proId);
						log.info("setOnClickListener() : message validate body is : " + msgBody);
						String message = msgBody +" "+validateCodeStr;
						QubecellSMSManager smsObj = QubecellSMSManager.getInstance();
						smsObj.sendMessage(appContext, message, smsShortCode, false);
						setProgressDisplayTime();
						setCurrentActivity(ApplicationActivities.CLOSE_ACTIVITY);
						setReceiveSmsonPort(0);
						showProgressDialogue("In Progress. . .");
					}
					else
					{
						log.info("nextButtonClick() : OTP code is : "+ validateCodeStr);
						List<NameValuePair> requestParam = new ArrayList<NameValuePair>();
						requestId = String.valueOf(CommonUtility.getRandomNumberBetween());
						String chargeKey = getchargeKey();
						String md5Str = null;
						if(chargeKey != null)
						{
							md5Str = getMD5(chargeKey+requestId);
						}
						else
						{
							log.error("Authentication key not found.");
						}
						if((operator == null) || (msisdn == null) || (transactionId == null))
						{
							log.error("ValidateOtp:: mendatory parameters not found.");
							return;
						}
						requestParam.add(new BasicNameValuePair(ConstantStrings.USERNAME, username));
						requestParam.add(new BasicNameValuePair(ConstantStrings.PASSWORD, password));
						requestParam.add(new BasicNameValuePair(ConstantStrings.REQUESTID, requestId));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATOR, operator));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATION, "validateotp"));
						requestParam.add(new BasicNameValuePair(ConstantStrings.SENDOTPTXNID, transactionId));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OTP, validateCodeStr));
						requestParam.add(new BasicNameValuePair(ConstantStrings.KEY, md5Str));
						requestParam.add(new BasicNameValuePair(ConstantStrings.MSISDN, msisdn));
						makeNetworkRequest(requestParam, ServerCommand.VALIDATEOTP_CMD);
					}
					validateCode.setText("");
				}
				else
				{
					displayToastMessage("Please provide valid one time password received via SMS.");
				}
			}
		});
	}

	/**
	 * This method is used to initialize the Elogger. 
	 */
	private void initLogger() 
	{
		if(log == null)
			log = new ELogger();
		log.setTag(logTag);
	}


	/**
	 * This method is used to make request to the network
	 * @param requestParam 
	 * @param requestParam
	 */
	private void makeNetworkRequest(final List<NameValuePair> requestParam, final int requestType) 
	{
		if(requestParam == null)
		{
			log.error("makeNetworkRequest() : Request Param is found null");
			return ;
		}
		new AsyncClient<Object[], Object, NetworkResponse>() 
		{
			@Override
			protected void onPreExecute() 
			{
				showProgressDialogue("In Progress. . .");
			};

			@Override
			protected NetworkResponse doInBackground(Object[]... arg0) 
			{
				NetworkController nwObj = new NetworkController();
				NetworkResponse netresp = nwObj.httpPost(requestParam, requestType);
				return netresp;
			}

			@Override
			protected void onPostExecute(NetworkResponse result) 
			{
				dismissProgressDialogue();
				handleServerResponse(result,requestType);
			};

		}.execute();
	}


	/**
	 * This method is used to handle the server response from server.
	 * @param result
	 */
	protected void handleServerResponse(NetworkResponse result, int requestType) 
	{
		if(result == null)
		{
			log.error("handleServerResponse() : Networkresponse not found");
			return;
		}

		if(result.netRespCode == NetworkResponseCode.NET_RESP_SUCCESS)
		{
			if(TextUtils.isEmpty(result.respStr))
			{
				log.error("handleServerResponse() : Response String is null");
				Intent intent = new Intent(ValidateOTPActivity.this, ResultActivity.class);
				intent.putExtra(IntentConstant.PAYMENT_RESULT, PaymentResult.FALIURE);
				intent.putExtra(IntentConstant.MESSAGE, ConstantStrings.TRANSACTION_CANNOT_PROCESS);
				startActivity(intent);
				finish();
				return;
			}
			String responseStr = result.respStr;
			XMLParser xmlObj = XMLParser.getInstance();
			ResponseBaseBean respBean = xmlObj.getResponseBean(responseStr, requestType);
			if(respBean == null)
			{
				log.error("onPostExecute() :  Msisdn bean is found null");
				Intent intent = new Intent(ValidateOTPActivity.this, ResultActivity.class);
				intent.putExtra(IntentConstant.PAYMENT_RESULT, PaymentResult.FALIURE);
				intent.putExtra(IntentConstant.MESSAGE, ConstantStrings.TRANSACTION_CANNOT_PROCESS);
				startActivity(intent);
				finish();
				return;
			}

			if(respBean.getResponsecode() == MsisdnServerRespCode.SUCCESS)
			{
				switch (requestType) {
				case ServerCommand.VALIDATEOTP_CMD:
				{
					if(operator != null && operator.equalsIgnoreCase(MobileOperators.IDEA))
					{
						Intent intent = new Intent(ValidateOTPActivity.this, ResultActivity.class);
						intent.putExtra(IntentConstant.PAYMENT_RESULT, PaymentResult.SUCCESS);
						intent.putExtra(IntentConstant.MESSAGE, ConstantStrings.THANKS_FOR_TRANSACTION);
						startActivity(intent);
						finish();
					}
					else
					{
						List<NameValuePair> requestParam = new ArrayList<NameValuePair>();
						requestId = String.valueOf(CommonUtility.getRandomNumberBetween());
						String chargeKey = getchargeKey();
						String md5Str = null;
						if(chargeKey != null)
						{
							md5Str = getMD5(chargeKey+requestId);
						}
						else
						{
							log.error("Authentication key not found.");
						}
						String productId;
						if((operator != null) && (msisdn != null))
						{
							productId = getProductId(operator);
							if(productId == null)
							{
								log.error(" product id not found.");
								return;
							}
						}
						else
						{
							return;
						}
						setChargedAmount("0");
						requestParam.add(new BasicNameValuePair(ConstantStrings.USERNAME, username));
						requestParam.add(new BasicNameValuePair(ConstantStrings.PASSWORD, password));
						requestParam.add(new BasicNameValuePair(ConstantStrings.REQUESTID, requestId));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATION, "eventcharge"));
						requestParam.add(new BasicNameValuePair(ConstantStrings.OPERATOR, operator));
						requestParam.add(new BasicNameValuePair(ConstantStrings.PRODUCTID, productId));
						requestParam.add(new BasicNameValuePair(ConstantStrings.MESSAGE, "Event charge request"));
						requestParam.add(new BasicNameValuePair(ConstantStrings.KEY, md5Str));
						requestParam.add(new BasicNameValuePair(ConstantStrings.MSISDN, msisdn));
						requestParam.add(new BasicNameValuePair(ConstantStrings.RETURNURL, ""));
						requestParam.add(new BasicNameValuePair(ConstantStrings.LOG_PATH, ""));
						makeNetworkRequest(requestParam, ServerCommand.EVENTCHARGE_CMD);
					}
				}
				break;

				case ServerCommand.EVENTCHARGE_CMD:
				{
					EventChargeRespBean eveChargeRespBean = (EventChargeRespBean)respBean; 
					String amount = eveChargeRespBean.getAmount();
					if(amount != null)
					{
						log.info("Validate operator event charge response. charged ampunt is : "+amount);
						setChargedAmount(amount);
					}
					else
					{
						log.error("Event charge payed ammount not found.");
					}
					if(operator != null && operator.equalsIgnoreCase(MobileOperators.IDEA))
					{ 
						log.info("ValidateOTPActivity : handleServerResponse : eventcharge.");

						transactionId = eveChargeRespBean.getTxnid();
					}
					else
					{
						String message = getCommandErrorMessage(requestType,respBean);
						if(message == null)
						{
							message = ConstantStrings.THANKS_FOR_TRANSACTION;
						}
						Intent intent = new Intent(ValidateOTPActivity.this, ResultActivity.class);
						intent.putExtra(IntentConstant.PAYMENT_RESULT, PaymentResult.SUCCESS);
						intent.putExtra(IntentConstant.MESSAGE, message);
						startActivity(intent);
						finish();
					}
				}
				break;

				case ServerCommand.SENDOTP_CMD:
				{
					SendOTPRespBean sendOTPBean = (SendOTPRespBean)respBean;
					requestId = sendOTPBean.getRequestid();
					transactionId = sendOTPBean.getTxnid();
				}
				break;

				default:
				{
					log.error("handleServerResponse() Invalida case");
				}
				break;
				} 
			}
			else
			{
				String message = getCommandErrorMessage(requestType,respBean);
				Intent intent = new Intent(ValidateOTPActivity.this, ResultActivity.class);
				intent.putExtra(IntentConstant.PAYMENT_RESULT, PaymentResult.FALIURE);
				intent.putExtra(IntentConstant.MESSAGE, message);
				startActivity(intent);
				finish();
			}
		}
		else
		{
			Intent intent = new Intent(ValidateOTPActivity.this, ResultActivity.class);
			intent.putExtra(IntentConstant.PAYMENT_RESULT, PaymentResult.FALIURE);
			intent.putExtra(IntentConstant.MESSAGE, ConstantStrings.TRANSACTION_CANNOT_PROCESS);
			startActivity(intent);
			finish();
		}
	}
}
