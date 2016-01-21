/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package at.kju.datacollector;
import android.content.Context; 

public class Constants {

	static int _accTypeId = 0;

	/**
	 * Account type string.
	 */
	public static int getAccTypeId(Context ctx) {
		if( _accTypeId == 0)
			_accTypeId = ctx.getResources().getIdentifier("aam_account_type", "id", ctx.getPackageName());
		return _accTypeId;
	}
	public static String getAccountType(Context ctx) {
		return (String) ctx.getResources().getText(getAccTypeId(ctx));
	}

	public static String getAuthTokenType(Context ctx) {
		return ctx.getPackageName();
	}

	public static String getContentAuthority(Context ctx) {
		return ctx.getPackageName();
	}

	public static String HTML_ROOT = "file:///android_asset/common/";

}
