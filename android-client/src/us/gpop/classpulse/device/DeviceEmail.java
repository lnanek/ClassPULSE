package us.gpop.classpulse.device;

import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;
import android.util.Patterns;

public class DeviceEmail {
	
	private static final String LOG_TAG = DeviceEmail.class.getSimpleName();
	
	private static final String TYPE = "com.google";

	public static String get(final Context context) {		
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(context).getAccounts();
		// Return Google account
		for (Account account : accounts) {
			Log.d(LOG_TAG, "checking account = " + account);

		    if (account.type.equals(TYPE) && emailPattern.matcher(account.name).matches()) {
		        if (null != account.name && !"".equals(account.name.trim()) ) {
		        	return account.name;
		        }
		    }
		}
		return null;		
	}
	
}
