# Copyright 2012 Citrix Systems, Inc. Licensed under the
# Apache License, Version 2.0 (the "License"); you may not use this
# file except in compliance with the License.  Citrix Systems, Inc.
# reserves all rights not expressly granted by the License.
# You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 
# Automatically generated by addcopyright.py at 04/03/2012
INSERT INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'default.page.size', '500', 'Default page size for API list* commands');
DELETE FROM `cloud`.`op_host_capacity` WHERE `capacity_type` in (2,6);

