// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// 
// Automatically generated by addcopyright.py at 04/03/2012
package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.PodResponse;
import com.cloud.dc.Pod;
import com.cloud.user.Account;

@Implementation(description="Updates a Pod.", responseObject=PodResponse.class)
public class UpdatePodCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdatePodCmd.class.getName());

    private static final String s_name = "updatepodresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the Pod")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the Pod")
    private String podName;

    @Parameter(name=ApiConstants.START_IP, type=CommandType.STRING, description="the starting IP address for the Pod")
    private String startIp;
    
    @Parameter(name=ApiConstants.END_IP, type=CommandType.STRING, description="the ending IP address for the Pod")
    private String endIp;
    
    @Parameter(name=ApiConstants.NETMASK, type=CommandType.STRING, description="the netmask of the Pod")
    private String netmask;

    @Parameter(name=ApiConstants.GATEWAY, type=CommandType.STRING, description="the gateway for the Pod")
    private String gateway;
    
    @Parameter(name=ApiConstants.ALLOCATION_STATE, type=CommandType.STRING, description="Allocation state of this cluster for allocation of new resources")
    private String allocationState;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getNetmask() {
        return netmask;
    }

    public String getEndIp() {
        return endIp;
    }

    public String getGateway() {
        return gateway;
    }

    public Long getId() {
        return id;
    }

    public String getPodName() {
        return podName;
    }

    public String getStartIp() {
        return startIp;
    }

    public String getAllocationState() {
    	return allocationState;
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute(){
        Pod result = _configService.editPod(this);
        if (result != null) {
            PodResponse response = _responseGenerator.createPodResponse(result,false);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update pod");
        }
    }
}
