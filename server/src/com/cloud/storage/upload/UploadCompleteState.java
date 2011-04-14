/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.storage.upload;

import com.cloud.agent.api.storage.UploadProgressCommand.RequestType;
import com.cloud.storage.Upload.Status;

public class UploadCompleteState extends UploadInactiveState {

	public UploadCompleteState(UploadListener ul) {
		super(ul);
	}

	@Override
	public String getName() {
		return Status.UPLOADED.toString();

	}


	@Override
	public void onEntry(String prevState, UploadEvent event, Object evtObj) {
		super.onEntry(prevState, event, evtObj);
		if (! prevState.equals(getName())) {
			if (event == UploadEvent.UPLOAD_ANSWER){
				getUploadListener().scheduleImmediateStatusCheck(RequestType.PURGE);
			}
			getUploadListener().setUploadInactive(Status.UPLOADED);
		}
		
	}
}
