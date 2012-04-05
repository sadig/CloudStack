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
package com.cloud.capacity;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.storage.Storage;
import com.cloud.utils.db.SearchCriteria;


public class StorageCapacityListener implements Listener {
    
    CapacityDao _capacityDao;
    float _overProvisioningFactor = 1.0f;    


    public StorageCapacityListener(CapacityDao _capacityDao,
            float _overProvisioningFactor) {
        super();
        this._capacityDao = _capacityDao;
        this._overProvisioningFactor = _overProvisioningFactor;
    }


    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }


    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }


    @Override
    public AgentControlAnswer processControlCommand(long agentId,
            AgentControlCommand cmd) {
        
        return null;
    }


    @Override
    public void processConnect(HostVO server, StartupCommand startup, boolean forRebalance) throws ConnectionException {
        
        if (!(startup instanceof StartupStorageCommand)) {
            return;
        }
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, server.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ,
                server.getDataCenterId());
        capacitySC.addAnd("podId", SearchCriteria.Op.EQ, server.getPodId());
        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);


        StartupStorageCommand ssCmd = (StartupStorageCommand) startup;
        if (ssCmd.getResourceType() == Storage.StorageResourceType.STORAGE_HOST) {
            CapacityVO capacity = new CapacityVO(server.getId(),
                    server.getDataCenterId(), server.getPodId(), server.getClusterId(), 0L,
                    (long) (server.getTotalSize() * _overProvisioningFactor),
                    CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED);
            _capacityDao.persist(capacity);
        }

    }


    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }


    @Override
    public boolean isRecurring() {
        return false;
    }

 
    @Override
    public int getTimeout() {
        return 0;
    }


    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

}
