package com.junkiesoup.care4clean;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.InputFilter;
import android.widget.EditText;

public class OkCancelInputDialog {

	AlertDialog.Builder alert;
	String userInput = "";
	Resources resources;
	
	public String getUserInput()
	{
		return userInput;
	}
	
	public OkCancelInputDialog(Context context, String title, String message, String defaultInput) {
		 alert = new AlertDialog.Builder(context);
		 alert.setTitle(title);
		 alert.setMessage(message);
		 final EditText input = new EditText(context);
		 // Limit input to 255 chars
		 InputFilter[] FilterArray = new InputFilter[1];
		 FilterArray[0] = new InputFilter.LengthFilter(255);
		 input.setFilters(FilterArray);

		 input.setText(defaultInput);
		 alert.setView(input);

		resources = context.getResources();

		alert.setPositiveButton(resources.getString(R.string.create_user_dialog_create_button), new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			userInput = input.getText().toString();
			clickOk();
			
		  }
		});

		alert.setNegativeButton(resources.getString(R.string.create_user_dialog_cancel_button), new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    clickCancel();
		  }
		});

	}
	
	public OkCancelInputDialog(Context context, String title, String message) {
		 alert = new AlertDialog.Builder(context);
		 alert.setTitle(title);
		 alert.setMessage(message);
		 final EditText input = new EditText(context);
		 alert.setView(input);
		 resources = context.getResources();



		alert.setPositiveButton(resources.getString(R.string.create_user_dialog_create_button), new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			userInput = input.getText().toString();
			clickOk();
			
		  }
		});

		alert.setNegativeButton(resources.getString(R.string.create_user_dialog_cancel_button), new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    clickCancel();
		  }
		});


	}
	
	public void clickOk()
	{
			
	}
	
	public void clickCancel()
	{
	}
	
	
	public void show()
	{
		alert.show();
	}
	

}
