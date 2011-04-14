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

package com.cloud.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.xen.resource.XenServerConnectionPool;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Session;

public class Db21to22MigrationUtil {
    private ClusterDao _clusterDao;
    private HostDao _hostDao;
    private AccountDao _accountDao;
    private DomainDao _domainDao;
    private ResourceCountDao _resourceCountDao;
    private InstanceGroupDao _vmGroupDao;
    private InstanceGroupVMMapDao _groupVMMapDao;
    private ConfigurationDao _configurationDao;
    private DataCenterDao _zoneDao;
    
    private void doMigration() {
        setupComponents();

        migrateResourceCounts();
        
        setupInstanceGroups();

        migrateZones();
        
        setupClusterGuid();
        
        System.out.println("Migration done");
    }
    
    /* add guid in cluster table */
    private void setupClusterGuid() {
        XenServerConnectionPool _connPool = XenServerConnectionPool.getInstance();
        List<ClusterVO> clusters = _clusterDao.listByHyTypeWithoutGuid(HypervisorType.XenServer.toString());
        for (ClusterVO cluster : clusters) {
            List<HostVO> hosts = _hostDao.listByCluster(cluster.getId());
            for (HostVO host : hosts) {
                String ip = host.getPrivateIpAddress();
                String username = host.getDetail("username");
                String password = host.getDetail("password");
                if (ip == null || ip.isEmpty() || username == null || username.isEmpty() || password == null
                        || password.isEmpty()) {
                    continue;
                }
                Connection conn = _connPool.slaveConnect(ip, username, password);
                if (conn == null)
                    continue;
                Pool.Record pr = null;
                try {
                    pr = XenServerConnectionPool.getPoolRecord(conn);
                } catch (Exception e) {
                    continue;
                } finally {
                    try {
                        Session.localLogout(conn);
                    } catch (Exception e) {
                    }
                }
                cluster.setGuid(pr.uuid);
                _clusterDao.update(cluster.getId(), cluster);
                break;
            }
        }
    }      
    
    /**
     * This method migrates the zones based on bug: 7204
     * based on the param direct.attach.untagged.vlan.enabled, we update zone to basic or advanced for 2.2
     */
    private void migrateZones(){
    	try {
			System.out.println("Migrating zones");
			String val = _configurationDao.getValue("direct.attach.untagged.vlan.enabled");
			NetworkType networkType;
			if(val == null || val.equalsIgnoreCase("true")){
				networkType = NetworkType.Basic;
			}else{
				networkType = NetworkType.Advanced;
			}
			List<DataCenterVO> existingZones = _zoneDao.listAll();
			for(DataCenterVO zone : existingZones){
				zone.setNetworkType(networkType);
				_zoneDao.update(zone.getId(), zone);
			}
		} catch (Exception e) {
			System.out.println("Unhandled exception in migrateZones()" + e);
		}
    }
    
    private void migrateResourceCounts() {
        System.out.println("migrating resource counts");
        SearchBuilder<ResourceCountVO> sb = _resourceCountDao.createSearchBuilder();
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);

        for (ResourceType type : ResourceType.values()) {
            SearchCriteria<ResourceCountVO> sc = sb.create();
            sc.setParameters("type", type);

            List<ResourceCountVO> resourceCounts = _resourceCountDao.search(sc, null);
            for (ResourceCountVO resourceCount : resourceCounts) {
                if (resourceCount.getAccountId() != null) {
                    Account acct = _accountDao.findById(resourceCount.getAccountId());
                    Long domainId = acct.getDomainId();
                    while (domainId != null) {
                        _resourceCountDao.updateDomainCount(domainId, type, true, resourceCount.getCount());
                        DomainVO domain = _domainDao.findById(domainId);
                        domainId = domain.getParent();
                    }
                }
            }
        }
        System.out.println("done migrating resource counts");
    }

    private void setupComponents() {
    	ComponentLocator locator = ComponentLocator.getLocator("migration", "migration-components.xml", "log4j-cloud.xml");

        _accountDao = locator.getDao(AccountDao.class);
        _domainDao = locator.getDao(DomainDao.class);
        _resourceCountDao = locator.getDao(ResourceCountDao.class);
        _vmGroupDao = locator.getDao(InstanceGroupDao.class);
        _groupVMMapDao = locator.getDao(InstanceGroupVMMapDao.class);
        _configurationDao = locator.getDao(ConfigurationDao.class);
        _zoneDao = locator.getDao(DataCenterDao.class);
    }
    
    private void setupInstanceGroups() {
    	System.out.println("setting up vm instance groups");
    	
    	//Search for all the vms that have not null groups
    	Long vmId = 0L;
    	Long accountId = 0L;
    	String groupName;
    	Transaction txn = Transaction.open(Transaction.CLOUD_DB);
    	txn.start();
		try {
	    	String request = "SELECT vm.id, uservm.account_id, vm.group from vm_instance vm, user_vm uservm where vm.group is not null and vm.removed is null and vm.id=uservm.id order by id";
	    	System.out.println(request);
	    	PreparedStatement statement = txn.prepareStatement(request);
	    	ResultSet result = statement.executeQuery();
	    	while (result.next()) {
	    		vmId = result.getLong(1);
	    		accountId = result.getLong(2);
	    		groupName = result.getString(3);
		        InstanceGroupVO group = _vmGroupDao.findByAccountAndName(accountId, groupName);
		    	//Create vm group if the group doesn't exist for this account
		        if (group == null) {
					group = new InstanceGroupVO(groupName, accountId);
					group =  _vmGroupDao.persist(group);
					System.out.println("Created new isntance group with name " + groupName + " for account id=" + accountId);
		        }
				
				if (group != null) {
					InstanceGroupVMMapVO groupVmMapVO = new InstanceGroupVMMapVO(group.getId(), vmId);
					_groupVMMapDao.persist(groupVmMapVO);
					System.out.println("Assigned vm id=" + vmId + " to group with name " + groupName + " for account id=" + accountId);
				}
	    	}
			txn.commit();
			statement.close();
		} catch (Exception e) {
			System.out.println("Unhandled exception: " + e);
		} finally {
			txn.close();
		}
    }


    public static void main(String[] args) {
        File file = PropertiesUtil.findConfigFile("log4j-cloud.xml");

        if (file != null) {
            System.out.println("Log4j configuration from : " + file.getAbsolutePath());
            DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
        } else {
            System.out.println("Configure log4j with default properties");
        }

        new Db21to22MigrationUtil().doMigration();
        System.exit(0);
    }
}
