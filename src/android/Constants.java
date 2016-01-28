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
	public static final String EXTRA_EVENT = "at.kju.datacollector.SyncService.Event";
	public static final String UPDATE_INTENT = "at.kju.datacollector.SYNC_ACTION" ;

	/**
	 * Account type string.
	 */

	static int _contentAuthId = 0;

	/**
	 * Account type string.
	 */
	public static int getContentAuthorityId(Context ctx) {
		if( _contentAuthId == 0)
			_contentAuthId = ctx.getResources().getIdentifier("content_authority", "string", ctx.getPackageName());
		return _contentAuthId;
	}

	public static String getContentAuthority(Context ctx) {
		return (String) ctx.getResources().getText(getContentAuthorityId(ctx));
	}

	public static String getExtraEvent(Context ctx) {
		return ctx.getPackageName() + ".SyncService.Event";
	}

	public static String getUpdateIntent(Context ctx)	{
		return ctx.getPackageName() + ".SYNC_ACTIONt";
	}

	public static String HTML_ROOT = "file:///android_asset/common/";

}
